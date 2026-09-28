package org.valle.process.models;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Builder
public record DecomposedSwagger(
        @Valid @NotNull SwaggerNode main,
        @Valid @Nullable SwaggerNode paths,
        @Valid @Nullable SwaggerNode components,
        @NotNull Map<String, String> componentCategories
) {
    /**
     * Backward-compatible constructor for callers that build a decomposed swagger directly.
     * An absent metadata map means that component files use the legacy schema convention.
     */
    public DecomposedSwagger(SwaggerNode main, SwaggerNode paths, SwaggerNode components) {
        this(main, paths, components, Map.of());
    }

    public DecomposedSwagger {
        componentCategories = componentCategories == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(componentCategories));
    }

    public Extension getExtension() {
        return this.main.extension();
    }

    /**
     * Name of the sidecar persisted next to {@code main.*}.  Keeping this outside the
     * OpenAPI document preserves the REST and CLI document contracts.
     */
    public static final String COMPONENT_CATEGORIES_FILE = "component-categories.json";
}
