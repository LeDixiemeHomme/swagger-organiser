package org.valle.present.rest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.InvalidPathException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Serves a small, fixed classpath directory without exposing files outside it.
 *
 * <p>The request path is decoded by {@link java.net.URI#getPath()}. Traversal
 * segments are rejected before the path is resolved so encoded traversal
 * attempts cannot escape the configured classpath root.</p>
 */
public final class ClasspathResourceHandler implements HttpHandler {

    private final String resourceRoot;
    private final String indexResource;

    public ClasspathResourceHandler(String resourceRoot, String indexResource) {
        if (resourceRoot == null || resourceRoot.isBlank()
                || !resourceRoot.startsWith("/") || resourceRoot.endsWith("/")) {
            throw new IllegalArgumentException("Le répertoire classpath doit commencer par '/' "
                    + "et ne pas se terminer par '/'.");
        }
        if (resourceRoot.equals("/") || hasTraversalSegment(resourceRoot.substring(1))) {
            throw new IllegalArgumentException("Le répertoire classpath est invalide.");
        }
        if (indexResource == null || indexResource.isBlank()
                || indexResource.startsWith("/") || indexResource.contains("..")) {
            throw new IllegalArgumentException("La ressource index classpath est invalide.");
        }
        this.resourceRoot = resourceRoot;
        this.indexResource = indexResource;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            RestUtils.sendError(exchange, 405, "Méthode non supportée — utilisez GET.");
            return;
        }

        String resourcePath = resolveResourcePath(exchange.getRequestURI().getPath());
        if (resourcePath == null) {
            RestUtils.sendError(exchange, 404, "Ressource introuvable.");
            return;
        }

        try (InputStream resource = getClass().getResourceAsStream(resourcePath)) {
            if (resource == null) {
                RestUtils.sendError(exchange, 404, "Ressource introuvable.");
                return;
            }
            byte[] content = resource.readAllBytes();
            RestUtils.sendBytes(exchange, 200, contentType(resourcePath), content);
        } catch (IOException exception) {
            RestUtils.sendError(exchange, 500, "Impossible de lire la ressource.");
        }
    }

    String resolveResourcePath(String requestPath) {
        if (requestPath == null
                || !(requestPath.equals(resourceRoot) || requestPath.startsWith(resourceRoot + "/"))) {
            return null;
        }

        String relative = requestPath.substring(resourceRoot.length());
        if (relative.isEmpty() || relative.equals("/")) {
            return resourceRoot + "/" + indexResource;
        }
        relative = relative.substring(1);

        if (relative.indexOf('\0') >= 0 || hasTraversalSegment(relative)) {
            return null;
        }
        Path normalized;
        try {
            normalized = Path.of(relative).normalize();
        } catch (InvalidPathException exception) {
            return null;
        }
        if (normalized.isAbsolute() || normalized.startsWith("..")
                || normalized.toString().equals(".")) {
            return null;
        }
        String normalizedResource = normalized.toString().replace('\\', '/');
        return resourceRoot + "/" + normalizedResource;
    }

    private static boolean hasTraversalSegment(String path) {
        return Arrays.stream(path.split("[/\\\\]"))
                .anyMatch(".."::equals);
    }

    private static String contentType(String resourcePath) {
        String extension = resourcePath.substring(resourcePath.lastIndexOf('.') + 1).toLowerCase();
        if ("html".equals(extension)) {
            return "text/html; charset=UTF-8";
        }
        if ("css".equals(extension)) {
            return "text/css; charset=UTF-8";
        }
        if ("js".equals(extension)) {
            return "text/javascript; charset=UTF-8";
        }
        if ("json".equals(extension)) {
            return "application/json; charset=UTF-8";
        }
        String guessed = URLConnection.guessContentTypeFromName(resourcePath);
        if (guessed != null) {
            return guessed + ("text/html".equals(guessed) || guessed.startsWith("text/")
                    ? "; charset=" + StandardCharsets.UTF_8.name() : "");
        }
        return "application/octet-stream";
    }
}
