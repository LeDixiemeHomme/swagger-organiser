package org.valle.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

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
    void should_serialize_a_node_using_the_requested_format() {
        ObjectNode node = new com.fasterxml.jackson.databind.ObjectMapper()
                .createObjectNode().put("openapi", "3.0.0");

        String yaml = new String(JacksonUtils.writeValueAsBytes(node, Extension.YML));

        assertThat(yaml).contains("openapi:", "3.0.0");
    }
}
