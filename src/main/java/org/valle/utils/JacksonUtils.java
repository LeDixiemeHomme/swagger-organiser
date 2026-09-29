package org.valle.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class JacksonUtils {

    private static final Pattern YAML_COMMENT_PATTERN = Pattern.compile("(^|\\s)(#.*)$");

    private JacksonUtils() {
    }

    public static SwaggerNode getSwaggerNode(File swaggerFile) {
        return getSwaggerNode(swaggerFile, true);
    }

    public static SwaggerNode getSwaggerNode(File swaggerFile, boolean preserveComments) {
        String content;
        try {
            content = Files.readString(swaggerFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read value from swagger file: " + swaggerFile.getPath(), e);
        }
        Extension extension = Extension.getSwaggerFileExtension(swaggerFile);
        log.debug("Swagger read from file: file={}, extension={}, preserveComments={}, "
                        + "comments={}, nodeComments={}, anchors={}",
                swaggerFile.getName(), extension, preserveComments,
                countComments(content), extractYamlCommentsByNode(content, extension).size(),
                extractYamlCommentsByAnchor(content, extension).size());
        SwaggerNode result = SwaggerNode.builder()
                .node(readValue(content, extension))
                .extension(extension)
                .comments(extractYamlComments(content, extension))
                .preserveComments(preserveComments)
                .commentsByNode(extractYamlCommentsByNode(content, extension))
                .rootComments(extractRootYamlComments(content, extension))
                .commentsByAnchor(extractYamlCommentsByAnchor(content, extension))
                .build();
        log.debug("Swagger node built: extension={}, preserveComments={}, comments={}, nodeComments={}, anchors={}",
                result.extension(), result.shouldPreserveComments(), countComments(result.comments()),
                result.commentsByNode().size(), result.commentsByAnchor().size());
        return result;
    }

    public static SwaggerNode getSwaggerNode(String swaggerString, Extension extension) {
        return getSwaggerNode(swaggerString, extension, true);
    }

    public static SwaggerNode getSwaggerNode(
            String swaggerString, Extension extension, boolean preserveComments) {
        SwaggerNode result = SwaggerNode.builder()
                .node(readValue(swaggerString, extension))
                .extension(extension)
                .comments(extractYamlComments(swaggerString, extension))
                .preserveComments(preserveComments)
                .commentsByNode(extractYamlCommentsByNode(swaggerString, extension))
                .rootComments(extractRootYamlComments(swaggerString, extension))
                .commentsByAnchor(extractYamlCommentsByAnchor(swaggerString, extension))
                .build();
        log.debug("Swagger node built from request: extension={}, preserveComments={}, comments={}, "
                        + "nodeComments={}, anchors={}",
                result.extension(), result.shouldPreserveComments(), countComments(result.comments()),
                result.commentsByNode().size(), result.commentsByAnchor().size());
        return result;
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
                    || !swaggerNode.shouldPreserveComments()
                    || swaggerNode.comments().isBlank()) {
                log.debug("Swagger serialization without comments: extension={}, preserveComments={}, "
                                + "comments={}, anchors={}, bytes={}",
                        swaggerNode.extension(), swaggerNode.shouldPreserveComments(),
                        countComments(swaggerNode.comments()), swaggerNode.commentsByAnchor().size(),
                        serialized.length);
                return serialized;
            }
            byte[] result = appendComments(serialized, swaggerNode.comments(), swaggerNode.commentsByAnchor());
            log.debug("Swagger serialization with comments: extension={}, comments={}, anchors={}, bytes={} -> {}",
                    swaggerNode.extension(), countComments(swaggerNode.comments()),
                    swaggerNode.commentsByAnchor().size(), serialized.length, result.length);
            return result;
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

    private static String extractRootYamlComments(String content, Extension extension) {
        if (extension == Extension.JSON) {
            return "";
        }
        StringBuilder comments = new StringBuilder();
        boolean sectionsStarted = false;
        for (String line : content.split("\\R")) {
            Matcher matcher = YAML_COMMENT_PATTERN.matcher(line);
            String trimmed = line.trim();
            if (line.length() - line.stripLeading().length() == 0
                    && (trimmed.equals("paths:") || trimmed.equals("components:"))) {
                sectionsStarted = true;
            }
            if (!sectionsStarted && matcher.find()
                    && line.stripLeading().equals(matcher.group(2).trim())) {
                if (comments.length() > 0) {
                    comments.append(System.lineSeparator());
                }
                comments.append(matcher.group(2).trim());
            }
        }
        return comments.toString();
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

    private static Map<String, String> extractYamlCommentsByNode(String content, Extension extension) {
        if (extension == Extension.JSON) {
            return Map.of();
        }
        Map<String, StringBuilder> comments = new LinkedHashMap<>();
        String section = null;
        String pending = null;
        String currentNodeKey = null;
        int currentNodeIndent = -1;
        int sectionIndent = -1;
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Matcher commentMatcher = YAML_COMMENT_PATTERN.matcher(line);
            if (commentMatcher.find() && trimmed.equals(commentMatcher.group(2).trim())) {
                pending = appendComment(pending, commentMatcher.group(2).trim());
                continue;
            }
            int indent = line.length() - line.stripLeading().length();
            if (indent == 0 && (trimmed.equals("paths:") || trimmed.equals("components:"))) {
                section = trimmed.substring(0, trimmed.length() - 1);
                pending = null;
                currentNodeKey = null;
                currentNodeIndent = -1;
                sectionIndent = indent;
                continue;
            }
            boolean nodeDeclaration = section != null
                    && trimmed.endsWith(":")
                    && ("paths".equals(section)
                    && indent > sectionIndent
                    && (currentNodeKey == null || indent == currentNodeIndent));
            if (nodeDeclaration) {
                String key = trimmed.substring(0, trimmed.length() - 1);
                currentNodeKey = section + "/" + ("paths".equals(section)
                        ? key.replaceFirst("^/", "").replace("/", "-").replace("{", "").replace("}", "")
                        : key);
                currentNodeIndent = indent;
                if (pending != null) {
                    comments.put(currentNodeKey, new StringBuilder(pending));
                }
                pending = null;
            } else if (pending != null && currentNodeKey != null && indent > currentNodeIndent) {
                comments.merge(currentNodeKey, new StringBuilder(pending),
                        (existing, additional) -> existing.append(System.lineSeparator()).append(additional));
                pending = null;
            } else if (indent == 0 && !trimmed.startsWith("#")) {
                section = null;
                pending = null;
                currentNodeKey = null;
                currentNodeIndent = -1;
            }
        }
        return comments.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> entry.getValue().toString()));
    }

    private static String appendComment(String current, String comment) {
        return current == null || current.isBlank()
                ? comment
                : current + System.lineSeparator() + comment;
    }

    private static long countComments(String content) {
        return content == null || content.isBlank()
                ? 0
                : content.lines().filter(line -> line.stripLeading().startsWith("#")).count();
    }

    private static byte[] appendComments(byte[] serialized, String comments) {
        String yaml = new String(serialized, java.nio.charset.StandardCharsets.UTF_8);
        String separator = comments.endsWith(System.lineSeparator())
                ? ""
                : System.lineSeparator();
        return (comments + separator + yaml)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] appendComments(byte[] serialized, String comments,
                                         Map<String, String> commentsByAnchor) {
        String yaml = new String(serialized, java.nio.charset.StandardCharsets.UTF_8);
        for (Map.Entry<String, String> entry : commentsByAnchor.entrySet()) {
            StringBuilder updated = new StringBuilder();
            boolean inserted = false;
            for (String line : yaml.split("\\R", -1)) {
                if (!inserted && line.trim().startsWith(entry.getKey() + ":")) {
                    String indentation = line.substring(0, line.length() - line.stripLeading().length());
                    updated.append(indentation).append(entry.getValue()).append(System.lineSeparator());
                    inserted = true;
                }
                updated.append(line).append(System.lineSeparator());
            }
            if (inserted) {
                yaml = updated.toString();
            }
        }
        String unanchored = comments;
        for (String comment : commentsByAnchor.values()) {
            unanchored = unanchored.replace(comment, "");
        }
        if (unanchored.isBlank()) {
            return yaml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        String separator = unanchored.endsWith(System.lineSeparator())
                ? ""
                : System.lineSeparator();
        return (unanchored + separator + yaml)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static Map<String, String> extractYamlCommentsByAnchor(
            String content, Extension extension) {
        if (extension == Extension.JSON) {
            return Map.of();
        }
        Map<String, String> anchors = new LinkedHashMap<>();
        String pending = null;
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            Matcher matcher = YAML_COMMENT_PATTERN.matcher(line);
            if (matcher.find() && trimmed.equals(matcher.group(2).trim())) {
                pending = appendComment(pending, matcher.group(2).trim());
            } else if (pending != null && trimmed.matches("[A-Za-z0-9_$.-]+:.*")) {
                anchors.put(trimmed.substring(0, trimmed.indexOf(':')), pending);
                pending = null;
            } else if (!trimmed.isEmpty()) {
                pending = null;
            }
        }
        return anchors;
    }
}
