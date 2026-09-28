package org.valle.present.rest;

import com.sun.net.httpserver.HttpExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.EndPoint;
import org.valle.process.models.Extension;
import org.valle.utils.ZipUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.net.URLDecoder;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * Utilitaires partagés entre les handlers REST.
 */
@Slf4j
class RestUtils {

    private RestUtils() {}

    // ── Lecture du fichier (raw ou multipart) ─────────────────────────────────

    static SwaggerRequest readSwaggerRequest(HttpExchange exchange, String emptyBodyMessage) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        return readSwaggerRequest(exchange, params, emptyBodyMessage);
    }

    static SwaggerRequest readSwaggerRequest(
            HttpExchange exchange, Map<String, String> params, String emptyBodyMessage) throws IOException {
        String extensionValue = requireQueryParameter(params, "extension",
                "Paramètre 'extension' manquant (json, yml, yaml).");
        byte[] body = readFileBytes(exchange);
        if (body.length == 0) {
            throw new IllegalArgumentException(emptyBodyMessage);
        }
        Extension extension = parseExtension(extensionValue);
        return new SwaggerRequest(params, extensionValue.trim().toLowerCase(), extension, body);
    }

    static byte[] readFileBytes(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        byte[] body;
        try (InputStream requestBody = exchange.getRequestBody()) {
            body = requestBody.readAllBytes();
        }

        log.debug("readFileBytes — Content-Type: {}, taille body: {} octets", contentType, body.length);

        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            String boundary = extractBoundary(contentType);
            log.debug("readFileBytes — multipart détecté, boundary: {}", boundary);
            byte[] fileBytes = extractMultipartFile(body, boundary);
            if (fileBytes == null) {
                throw new IllegalArgumentException(
                        "Aucune partie 'file' trouvée dans le corps multipart.");
            }
            log.debug("readFileBytes — fichier extrait du multipart: {} octets", fileBytes.length);
            return fileBytes;
        }
        return body;
    }

    private static String extractBoundary(String contentType) {
        for (String part : contentType.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("boundary=")) {
                String b = trimmed.substring("boundary=".length()).trim();
                if (b.length() >= 2 && b.startsWith("\"") && b.endsWith("\"")) {
                    b = b.substring(1, b.length() - 1);
                }
                if (!b.isBlank()) {
                    return b;
                }
            }
        }
        throw new IllegalArgumentException("Boundary manquant dans Content-Type : " + contentType);
    }

    private static byte[] extractMultipartFile(byte[] body, String boundary) {
        byte[] delimiter  = ("\r\n--" + boundary).getBytes(StandardCharsets.UTF_8);
        byte[] firstBound = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        byte[] CRLFCRLF   = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);

        int pos = indexOfBytes(body, firstBound, 0);
        if (pos < 0) {
            log.warn("extractMultipartFile — boundary '{}' introuvable dans le body", boundary);
            return null;
        }
        pos += firstBound.length;
        if (pos + 1 < body.length && body[pos] == '\r' && body[pos + 1] == '\n') pos += 2;

        byte[] fallback = null;

        while (pos < body.length) {
            int headersEnd = indexOfBytes(body, CRLFCRLF, pos);
            if (headersEnd < 0) break;

            String headers = new String(body, pos, headersEnd - pos, StandardCharsets.UTF_8);
            int contentStart = headersEnd + 4;
            int contentEnd   = indexOfBytes(body, delimiter, contentStart);
            if (contentEnd < 0) contentEnd = body.length;

            log.debug("extractMultipartFile — partie: [{}]", headers.replace("\r\n", " | "));

            byte[] partBytes = Arrays.copyOfRange(body, contentStart, contentEnd);

            if (headers.contains("name=\"file\""))  return partBytes;
            if (fallback == null && headers.contains("filename=")) fallback = partBytes;

            pos = contentEnd + delimiter.length;
            if (pos + 1 < body.length && body[pos] == '\r' && body[pos + 1] == '\n') pos += 2;
        }
        return fallback;
    }

    private static int indexOfBytes(byte[] source, byte[] target, int from) {
        outer:
        for (int i = from; i <= source.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) continue outer;
            }
            return i;
        }
        return -1;
    }


    // ── Construction du ZIP ───────────────────────────────────────────────────

    static byte[] buildZip(DecomposedSwagger decomposed) throws IOException {
        return ZipUtils.build(decomposed);
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    static String requireQueryParameter(Map<String, String> params, String name, String message) {
        String value = params.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    static Set<EndPoint> parseEndpoints(String endpoints) {
        if (endpoints == null || endpoints.isBlank()) {
            throw new IllegalArgumentException(
                    "La liste des endpoints ne peut pas être vide (ex: get:/path,post:/path2).");
        }
        Set<EndPoint> parsed = Arrays.stream(endpoints.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(EndPoint::fromString)
                .collect(Collectors.toSet());
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException(
                    "La liste des endpoints ne peut pas être vide (ex: get:/path,post:/path2).");
        }
        return parsed;
    }

    static Extension parseExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("Extension must be json, yml or yaml.");
        }
        try {
            return Extension.valueOf(extension.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Extension must be json, yml or yaml.", e);
        }
    }

    static String resolveContentType(Extension extension) {
        return switch (extension) {
            case JSON      -> "application/json";
            case YML, YAML -> "application/yaml";
        };
    }

    static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        byte[] body = mapper.createObjectNode()
                .put("code", errorCode(code))
                .put("message", message)
                .toString()
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static String errorCode(int status) {
        return switch (status) {
            case 400 -> "INVALID_REQUEST";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            default -> status >= 500 ? "INTERNAL_ERROR" : "HTTP_ERROR";
        };
    }

    static void sendBytes(HttpExchange exchange, int code, String contentType, byte[] body) throws IOException {
        if (body == null) {
            throw new IllegalArgumentException("La réponse ne peut pas être null.");
        }
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    static void logInvalidRequest(String operation, Exception exception) {
        log.warn("REST {} — requête invalide : {}", operation, exception.getMessage());
    }

    static void logInternalError(String operation, Exception exception) {
        log.error("REST {} — erreur inattendue", operation, exception);
    }

    static void extractZip(byte[] zipBytes, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(
                new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(targetDir)) {
                    throw new IllegalArgumentException(
                            "Entrée ZIP invalide (Zip Slip détecté) : " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Path parent = entryPath.getParent();
                    if (parent == null) {
                        throw new IllegalArgumentException(
                                "Entrée ZIP invalide : " + entry.getName());
                    }
                    Files.createDirectories(parent);
                    Files.write(entryPath, zis.readAllBytes());
                }
                zis.closeEntry();
            }
        } catch (ZipException e) {
            throw new IllegalArgumentException("Le corps de la requête n'est pas une archive ZIP valide.", e);
        }
    }

    static File findMainFile(Path dir) {
        for (String name : new String[]{"main.yml", "main.yaml", "main.json"}) {
            java.io.File candidate = dir.resolve(name).toFile();
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }

    static void deleteRecursively(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("REST — impossible de supprimer : {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("REST — impossible de supprimer le répertoire temporaire : {}", dir, e);
        }
    }
}
