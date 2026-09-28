package org.valle.process.models;

import com.fasterxml.jackson.databind.JsonNode;

public record DollarRef(
        String rawValue
) {
    /**
     * Take $ref and get the name of the schema from it.
     * exemple : with #/components/schemas/ReferenceObjectName
     * the string ReferenceObjectName is returned.
     *
     * @return the name of the schema.
     */
    public String getReferencedName() {
        String[] parts = referenceParts();
        return parts[parts.length - 1];
    }

    public String getComponentFileReference() {
        String[] parts = referenceParts();
        return "../components/%s".formatted(parts[parts.length - 1]);
    }

    public String getPathFileReference() {
        String[] parts = referenceParts();
        return "../paths/%s".formatted(parts[parts.length - 1]);
    }

    public JsonNode getReferencedNode(JsonNode jsonNode) {
        String[] parts = referenceParts();
        JsonNode currentNode = jsonNode;
        for (String part : parts) {
            if (!part.isEmpty() && !part.equals("#")) {
                if (currentNode == null) {
                    return null;
                }
                currentNode = currentNode.get(part);
            }
        }
        return currentNode;
    }

    private String[] referenceParts() {
        if (rawValue == null || rawValue.isBlank() || rawValue.endsWith("/")) {
            throw new IllegalArgumentException("Invalid $ref value: " + rawValue);
        }
        return rawValue.split("/");
    }
}
