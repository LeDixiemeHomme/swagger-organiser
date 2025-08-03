package org.valle.process.models;

public record DollarRef(
        String rawValue
) {
    /**
     * Take $ref and get the name of the schema from it.
     *
     * @return the name of the schema.
     */
    public String getName() {
        return rawValue.replaceAll("^'|'$", "")
                .replaceAll("^\"|\"$", "")
                .replaceAll("#/components/schemas/", "");
    }
}
