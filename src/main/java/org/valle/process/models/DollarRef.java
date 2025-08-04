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
        String[] parts = rawValue.split("/");
        return parts[parts.length - 1];
    }

    public JsonNode getReferencedNode(JsonNode jsonNode) {
        String[] parts = rawValue.split("/");
        JsonNode currentNode = jsonNode;
        for (String part : parts) {
            if (!part.isEmpty() && !part.equals("#")) {
                currentNode = currentNode.get(part);
            }
        }
        return currentNode;
    }
}
