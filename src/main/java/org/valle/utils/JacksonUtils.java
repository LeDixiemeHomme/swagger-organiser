package org.valle.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.Getter;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;

import java.io.File;

public class JacksonUtils {

    private final File swaggerFile;

    @Getter
    private final ObjectMapper mapper;

    public JacksonUtils(File swaggerFile) {
        this.swaggerFile = swaggerFile;
        this.mapper = new ObjectMapper(this.getParseFactory());
        // active l'indentation pour les json
        this.mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
    }

    public SwaggerNode getSwaggerNode() {
        return SwaggerNode.builder()
                .node(this.readValue())
                .extension(this.getSwaggerFileExtension())
                .build();
    }

    public JsonNode readValue() {
        try {
            return this.mapper.readTree(this.swaggerFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read value from swagger file: " + swaggerFile.getPath(), e);
        }
    }

    public void writeValue(JsonNode node) {
        try {
            this.mapper.writeValue(this.swaggerFile, node);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write value to swagger file: " + swaggerFile.getPath(), e);
        }
    }

    public Extension getSwaggerFileExtension() {
        String path = this.swaggerFile.getPath();

        if (path.endsWith(".yml")) return Extension.YML;

        if (path.endsWith(".yaml")) return Extension.YAML;

        if (path.endsWith(".json")) return Extension.JSON;

        throw new IllegalArgumentException("Unsupported file type: " + path);
    }

    /**
     * YAMLFactory hérite de JsonFactory.
     *
     * @return Une factory JSON ou YAML en fonction de l'extension du fichier swagger
     */
    public JsonFactory getParseFactory() {
        Extension extension = this.getSwaggerFileExtension();
        return switch (extension) {
            case YML, YAML -> new YAMLFactory()
                    // generation sans les --- au début du fichier
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
            case JSON -> new JsonFactory();
        };
    }
}
