package org.valle.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JacksonUtils {

    private static final Pattern YAML_COMMENT_PATTERN = Pattern.compile("(^|\\s)(#.*)$");

    private JacksonUtils() {
    }

    public static SwaggerNode getSwaggerNode(File swaggerFile) {
        String content;
        try {
            content = Files.readString(swaggerFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read value from swagger file: " + swaggerFile.getPath(), e);
        }
        return SwaggerNode.builder()
                .node(readValue(content, Extension.getSwaggerFileExtension(swaggerFile)))
                .extension(Extension.getSwaggerFileExtension(swaggerFile))
                .comments(extractYamlComments(content, Extension.getSwaggerFileExtension(swaggerFile)))
                .build();
    }

    public static SwaggerNode getSwaggerNode(String swaggerString, Extension extension) {
        return SwaggerNode.builder()
                .node(readValue(swaggerString, extension))
                .extension(extension)
                .comments(extractYamlComments(swaggerString, extension))
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
            byte[] serialized = createMapper(swaggerNode.extension()).writeValueAsBytes(swaggerNode.node());
            if (swaggerNode.extension() == Extension.JSON
                    || swaggerNode.comments().isBlank()) {
                return serialized;
            }
            return appendComments(serialized, swaggerNode.comments());
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

    public static void writeValue(File swaggerFile, SwaggerNode swaggerNode) {
        try {
            Files.write(swaggerFile.toPath(), writeValueAsBytes(swaggerNode));
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

    private static String extractYamlComments(String content, Extension extension) {
        if (extension == Extension.JSON) {
            return "";
        }
        StringBuilder comments = new StringBuilder();
        for (String line : content.split("\\R")) {
            Matcher matcher = YAML_COMMENT_PATTERN.matcher(line);
            if (matcher.find()) {
                if (comments.length() > 0) {
                    comments.append(System.lineSeparator());
                }
                comments.append(matcher.group(2).trim());
            }
        }
        return comments.toString();
    }

    private static byte[] appendComments(byte[] serialized, String comments) {
        String yaml = new String(serialized, java.nio.charset.StandardCharsets.UTF_8);
        String separator = yaml.endsWith(System.lineSeparator()) ? "" : System.lineSeparator();
        return (yaml + separator + comments + System.lineSeparator())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
