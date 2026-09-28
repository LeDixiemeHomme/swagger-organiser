package org.valle.present.rest;

import org.valle.process.models.Extension;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Validated common input for REST operations receiving a Swagger document.
 */
record SwaggerRequest(Map<String, String> parameters, String extensionValue, Extension extension, byte[] body) {

    String content() {
        return new String(body, StandardCharsets.UTF_8);
    }

    String outputFilename(String prefix) {
        return prefix + "." + extensionValue;
    }
}
