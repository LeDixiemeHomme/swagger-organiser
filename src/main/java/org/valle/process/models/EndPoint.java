package org.valle.process.models;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record EndPoint(
        @NotNull String method,
        @NotNull String path,
        @Nullable String requestBody,
        @NotNull List<String> linkedSchemas
) {
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EndPoint other = (EndPoint) obj;
        return method.equals(other.method) && path.equals(other.path);
    }
}
