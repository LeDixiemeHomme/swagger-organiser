package org.valle.process.models;

import java.io.File;

public enum Extension {
    YML, YAML, JSON;

    public static Extension getSwaggerFileExtension(File swaggerFile) {
        String path = swaggerFile.getPath();

        if (path.endsWith(".yml")) return Extension.YML;

        if (path.endsWith(".yaml")) return Extension.YAML;

        if (path.endsWith(".json")) return Extension.JSON;

        throw new IllegalArgumentException("Unsupported file type: " + path);
    }
}
