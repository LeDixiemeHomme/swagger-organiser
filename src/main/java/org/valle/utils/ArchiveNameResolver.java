package org.valle.utils;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;

/**
 * Resolves safe archive and entry names for generated Swagger results.
 */
public final class ArchiveNameResolver {

    private ArchiveNameResolver() {
    }

    public static String resolve(String requestedName, JsonNode swagger, String operation, String fallback) {
        String candidate = requestedName == null || requestedName.isBlank()
                ? titleOf(swagger)
                : requestedName;
        String safeCandidate = sanitize(candidate);
        if (safeCandidate.isBlank()) {
            return fallback;
        }
        return safeCandidate + "-" + operation;
    }

    private static String titleOf(JsonNode swagger) {
        if (swagger == null) {
            return "";
        }
        return swagger.path("info").path("title").asText("");
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.trim()
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^[.\\-]+|[.\\-]+$", "");
        return sanitized.toLowerCase(Locale.ROOT).equals(".") ? "" : sanitized;
    }
}
