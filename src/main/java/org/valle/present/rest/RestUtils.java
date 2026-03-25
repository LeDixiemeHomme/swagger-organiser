package org.valle.present.rest;

import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.Extension;
import org.valle.utils.ZipUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaires partagés entre les handlers REST.
 */
@Slf4j
class RestUtils {

    private RestUtils() {}

    // ── Lecture du fichier (raw ou multipart) ─────────────────────────────────

    static byte[] readFileBytes(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        byte[] body = exchange.getRequestBody().readAllBytes();

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
                return b.startsWith("\"") ? b.substring(1, b.length() - 1) : b;
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
            if (idx > 0) params.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return params;
    }

    static String resolveContentType(Extension extension) {
        return switch (extension) {
            case JSON      -> "application/json";
            case YML, YAML -> "application/yaml";
        };
    }

    static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    static void sendBytes(HttpExchange exchange, int code, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}

