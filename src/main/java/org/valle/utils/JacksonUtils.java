package org.valle.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;

import java.io.File;

public class JacksonUtils {

    private JacksonUtils() {
    }

    public static SwaggerNode getSwaggerNode(File swaggerFile) {
        return SwaggerNode.builder()
                .node(readValue(swaggerFile))
                .extension(Extension.getSwaggerFileExtension(swaggerFile))
                .build();
    }

    public static SwaggerNode getSwaggerNode(String swaggerString, Extension extension) {
        return SwaggerNode.builder()
                .node(readValue(swaggerString, extension))
                .extension(extension)
                .build();
    }

    public static JsonNode readValue(File swaggerFile) {
        try {
            Extension extension = Extension.getSwaggerFileExtension(swaggerFile);
            return createMapper(extension).readTree(swaggerFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read value from swagger file: " + swaggerFile.getPath(), e);
        }
    }

    public static byte[] writeValueAsBytes(SwaggerNode swaggerNode) {
        try {
            return createMapper(swaggerNode.extension()).writeValueAsBytes(swaggerNode.node());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize SwaggerNode to bytes", e);
        }
    }

    public static byte[] writeValueAsBytes(JsonNode node, Extension extension) {
        try {
            return createMapper(extension).writeValueAsBytes(node);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JsonNode to bytes", e);
        }
    }

    public static void writeValue(File swaggerFile, JsonNode node) {
        try {
            Extension extension = Extension.getSwaggerFileExtension(swaggerFile);
            createMapper(extension).writeValue(swaggerFile, node);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write value to swagger file: " + swaggerFile.getPath(), e);
        }
    }

    public static JsonNode readValue(String swaggerString, Extension extension) {
        try {
            JsonNode node = createMapper(extension).readTree(swaggerString);
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException(
                        "Swagger invalide : le document doit être un objet JSON ou YAML.");
            }
            return node;
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException illegalArgumentException
                    && illegalArgumentException.getMessage().startsWith("Swagger invalide")) {
                throw illegalArgumentException;
            }
            throw new IllegalArgumentException(
                    "Swagger invalide : impossible de lire le contenu fourni.", e);
        }
    }

    /**
     * YAMLFactory hérite de JsonFactory.
     *
     * @return Une factory JSON ou YAML en fonction de l'extension du fichier swagger
     */
    static JsonFactory getParseFactory(Extension extension) {
        return switch (extension) {
            case YML, YAML -> new YAMLFactory()
                    // generation sans les --- au début du fichier
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                    // Désactive l'insertion d'antislashs '\' et le split de lignes longues
                    .disable(YAMLGenerator.Feature.SPLIT_LINES);
            case JSON -> new JsonFactory();
        };
    }

    static ObjectMapper createMapper(Extension extension) {
        ObjectMapper mapper = new ObjectMapper(getParseFactory(extension));
        // active l'indentation pour les json
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }
}
