package org.valle.process.models;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record DecomposedSwagger(
        @Valid @NotNull SwaggerNode main,
        @Valid @Nullable SwaggerNode paths,
        @Valid @Nullable SwaggerNode components
) {
    public Extension getExtension() {
        return this.main.extension();
    }
}
