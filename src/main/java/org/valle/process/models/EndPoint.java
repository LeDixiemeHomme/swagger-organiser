package org.valle.process.models;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record EndPoint(
        @NotNull String method,
        @NotNull String path
) {
    public static EndPoint fromString(String string) {
        if (string == null || string.isBlank()) {
            throw new IllegalArgumentException("Endpoint must use the format method:path.");
        }

        String[] parts = string.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException(
                    "Endpoint must use the format method:path: " + string);
        }

        return EndPoint.builder()
                .method(parts[0].trim())
                .path(parts[1].trim())
                .build();
    }
}
