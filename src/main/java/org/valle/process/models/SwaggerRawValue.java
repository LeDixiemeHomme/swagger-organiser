package org.valle.process.models;

import lombok.Builder;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Builder(toBuilder = true)
public record SwaggerRawValue(
        Map<String, Object> rawValue,
        Extension extension
) {

    public Map<String, Object> removeElementsByName(
            Set<EndPoint> endPointsToRemove,
            Set<String> schemasToRemove
    ) {
        Map<String, Object> paths = (Map<String, Object>) this.rawValue().get("paths");
        for (EndPoint endPoint : endPointsToRemove) {
            Iterator<Map.Entry<String, Object>> pathIterator = paths.entrySet().iterator();
            while (pathIterator.hasNext()) {
                Map.Entry<String, Object> pathEntry = pathIterator.next();
                if (pathEntry.getKey().equals(endPoint.path())) {
                    Map<String, Object> methods = (Map<String, Object>) pathEntry.getValue();
                    Iterator<Map.Entry<String, Object>> methodIterator = methods.entrySet().iterator();
                    while (methodIterator.hasNext()) {
                        Map.Entry<String, Object> methodEntry = methodIterator.next();
                        if (methodEntry.getKey().equals(endPoint.method())) {
                            methodIterator.remove();
                        }
                    }
                    if (methods.isEmpty()) {
                        pathIterator.remove();
                    }
                }
            }
        }
        Map<String, Object> components = (Map<String, Object>) this.rawValue().get("components");
        Iterator<Map.Entry<String, Object>> componentIterator = components.entrySet().iterator();

        while (componentIterator.hasNext()) {
            Map.Entry<String, Object> componentEntry = componentIterator.next();
            Map<String, Object> componentValue = (Map<String, Object>) componentEntry.getValue();
            schemasToRemove.forEach(componentValue::remove);
        }

        return this.rawValue();
    }

    public SwaggerRawValue removeComponents() {
        Map<String, Object> components = (Map<String, Object>) this.rawValue().get("components");
        if (components != null) {
            components.clear();
        }
        return this;
    }

    public SwaggerRawValue changePathReferences() {
        Map<String, Object> copy = new java.util.HashMap<>(this.rawValue());

        Map<String, Object> paths = (Map<String, Object>) copy.get("paths");
        paths.entrySet().stream().forEach(entry -> {
            String key = entry.getKey();
            String withoutFirstSlash = key.startsWith("/") ? key.substring(1) : key;
            String ref = withoutFirstSlash.replace("/", "-").replace("{", "").replace("}", "");
            Map<String, Object> mapRef = Map.of(
                    "$ref",
                    "paths/%s.%s".formatted(ref, this.extension.name().toLowerCase())
            );
            entry.setValue(mapRef);
        });
        return this.toBuilder().rawValue(copy).build();
    }

    public SwaggerRawValue changeComponentsReferences() {
        Map<String, Object> copy = new java.util.HashMap<>(this.rawValue());

        Map<String, Object> components = (Map<String, Object>) copy.get("components");
        components.entrySet().stream().forEach(entry -> {
            Map<String, Object> map = (Map<String, Object>) entry.getValue();
            changeComponentElementsReferences(map);
        });
        return this.toBuilder().rawValue(copy).build();
    }

    private void changeComponentElementsReferences(Map<String, Object> componentElement) {
        componentElement.entrySet().stream().forEach(entry -> {
            String formatted = "../components/%s.%s".formatted(entry.getKey(), this.extension.name().toLowerCase());
            Map<String, Object> mapRef = Map.of(
                    "$ref",
                    formatted
            );
            entry.setValue(mapRef);
        });
    }
}
