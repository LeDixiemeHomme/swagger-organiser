package org.valle.present.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.valle.process.models.Extension;
import org.valle.utils.ArchiveNameResolver;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Validated common input for REST operations receiving a Swagger document.
 */
record SwaggerRequest(
        Map<String, String> parameters,
        String extensionValue,
        Extension extension,
        boolean preserveComments,
        String archiveName,
        byte[] body) {

    String content() {
        return new String(body, StandardCharsets.UTF_8);
    }

    String outputFilename(String fallbackPrefix, JsonNode swagger) {
        return ArchiveNameResolver.resolve(archiveName, swagger, operation(fallbackPrefix),
                fallbackPrefix) + "." + extensionValue;
    }

    String archiveFilename(String fallbackPrefix, JsonNode swagger) {
        return ArchiveNameResolver.resolve(archiveName, swagger, operation(fallbackPrefix),
                fallbackPrefix) + ".zip";
    }

    private static String operation(String fallbackPrefix) {
        int separator = fallbackPrefix.lastIndexOf('-');
        return separator < 0 ? fallbackPrefix : fallbackPrefix.substring(separator + 1);
    }
}
