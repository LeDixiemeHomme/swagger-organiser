package org.valle.process.models;

public record DollarRef(
        String rawValue
) {
    /**
     * Take $ref and get the name of the schema from it.
     * handle #/components/schemas/
     * handle #/components/parameters/
     *
     * @return the name of the schema.
     */
    public String getName() {
        return rawValue.replaceAll("^'|'$", "")
                .replaceAll("^\"|\"$", "")
                .replaceAll("#/components/parameters/", "")
                .replaceAll("#/components/schemas/", "");
    }
}
