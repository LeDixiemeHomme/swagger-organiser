package org.valle.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void should_read_json_string_with_its_extension() {
        SwaggerNode result = JacksonUtils.getSwaggerNode("{\"openapi\":\"3.0.0\"}", Extension.JSON);

        assertThat(result.extension()).isEqualTo(Extension.JSON);
        assertThat(result.node().get("openapi").asText()).isEqualTo("3.0.0");
    }

    @Test
    void should_read_yaml_file_and_detect_its_extension() throws Exception {
        Path file = tempDir.resolve("swagger.yml");
        Files.writeString(file, "openapi: 3.0.0\n");

        SwaggerNode result = JacksonUtils.getSwaggerNode(file.toFile());

        assertThat(result.extension()).isEqualTo(Extension.YML);
        assertThat(result.node().get("openapi").asText()).isEqualTo("3.0.0");
    }

    @Test
    void should_preserve_yaml_comments_when_serializing_a_swagger_node() {
        String source = """
                # API documentation
                openapi: 3.0.0
                info:
                  title: Example # Public API
                  version: 1.0.0
                """;

        SwaggerNode swaggerNode = JacksonUtils.getSwaggerNode(source, Extension.YML);

        String yaml = new String(JacksonUtils.writeValueAsBytes(swaggerNode));

        assertThat(yaml)
                .contains("# API documentation")
                .contains("# Public API");
    }

    @Test
    void should_keep_comment_before_the_following_yaml_property() {
        String source = """
                openapi: 3.0.1
                info:
                    title: DECISEO-API
                    # todo test a delete
                    description: >-
                        Swagger for DECISEO API
                    version: v1.3.0
                """;

        String yaml = new String(JacksonUtils.writeValueAsBytes(
                JacksonUtils.getSwaggerNode(source, Extension.YML)));

        assertThat(yaml.indexOf("title:")).isLessThan(yaml.indexOf("# todo test a delete"));
        assertThat(yaml.indexOf("# todo test a delete"))
                .isLessThan(yaml.indexOf("description:"));
    }

    @Test
    void should_associate_nested_path_comment_with_its_path_file() {
        String source = """
                openapi: 3.0.1
                paths:
                  /entities/{customer_id}/survey:
                    # todo test a delete
                    get:
                      tags:
                        - Survey
                """;

        SwaggerNode node = JacksonUtils.getSwaggerNode(source, Extension.YML);

        assertThat(node.commentsByNode()).containsKey("paths/entities-customer_id-survey");
        assertThat(node.commentsFor("paths/entities-customer_id-survey"))
                .contains("# todo test a delete");
    }

    @Test
    void should_preserve_yaml_comments_when_serializing_a_swagger_node_real_case() {
        String source = """
                title: "PilotedProfileV2"
                type: "object"
                #todo description
                required:
                    - "label"
                    - "code"
                    - "is_recommended"
                properties:
                    label:
                        type: "string"
                        description: "Libellé du profil proposé pour le mode Pilotee"
                        example: "dynamique"
                    code:
                        type: "string"
                        description: "Code du profil associé au profil"
                        example: "5"
                    is_recommended:
                        type: "boolean"
                        description: "Indique si ce profil est celui recommandé parmi la liste proposée."
                        example: true
                """;

        SwaggerNode swaggerNode = JacksonUtils.getSwaggerNode(source, Extension.YML);

        String yaml = new String(JacksonUtils.writeValueAsBytes(swaggerNode));

        assertThat(yaml)
                .contains("#todo description");
        assertThat(yaml.indexOf("#todo description"))
                .isLessThan(yaml.indexOf("required:"));
    }

    @Test
    void should_omit_yaml_comments_when_preserve_comments_is_disabled() {
        String source = """
                # API documentation
                openapi: 3.0.0
                info:
                  title: Example # Public API
                """;

        SwaggerNode swaggerNode = JacksonUtils.getSwaggerNode(source, Extension.YML, false);

        String yaml = new String(JacksonUtils.writeValueAsBytes(swaggerNode));

        assertThat(yaml)
                .doesNotContain("# API documentation")
                .doesNotContain("# Public API");
    }

    @Test
    void should_serialize_a_node_using_the_requested_format() {
        ObjectNode node = new com.fasterxml.jackson.databind.ObjectMapper()
                .createObjectNode().put("openapi", "3.0.0");

        String yaml = new String(JacksonUtils.writeValueAsBytes(node, Extension.YML));

        assertThat(yaml).contains("openapi:", "3.0.0");
    }

    @Test
    void should_reject_invalid_swagger_content_as_a_client_error() {
        assertThatThrownBy(() -> JacksonUtils.getSwaggerNode("{not-json", Extension.JSON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Swagger invalide : impossible de lire le contenu fourni.");
    }

    @Test
    void should_reject_non_object_swagger_content() {
        assertThatThrownBy(() -> JacksonUtils.getSwaggerNode("[]", Extension.JSON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Swagger invalide : le document doit être un objet JSON ou YAML.");
    }
}
