package org.valle.process;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.fromfile.jackson.GetSwaggerNodeJacksonFromFileImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MergeSwaggerImplTest {

    @TempDir
    Path tempDir;

    @Test
    void should_inline_paths_and_collect_component_schemas() throws IOException {
        write("main.yml", """
                openapi: 3.0.0
                info:
                  title: Test
                  version: 1.0.0
                paths:
                  /users:
                    $ref: paths/users.yml
                """);
        write("paths/users.yml", """
                get:
                  responses:
                    '200':
                      description: OK
                      content:
                        application/json:
                          schema:
                            $ref: ../components/User.yml
                """);
        write("components/User.yml", """
                type: object
                properties:
                  id:
                    type: string
                """);

        SwaggerNode result = merge().execute();
        JsonNode root = result.node();

        assertThat(root.at("/paths/~1users/get/responses/200/content/application~1json/schema/$ref")
                .asText()).isEqualTo("#/components/schemas/User");
        assertThat(root.at("/components/schemas/User/type").asText()).isEqualTo("object");
        assertThat(root.at("/paths/~1users/$ref").isMissingNode()).isTrue();
    }

    @Test
    void should_preserve_internal_refs_and_inline_direct_component_refs() throws IOException {
        write("main.yml", """
                openapi: 3.0.0
                info:
                  title: Test
                  version: 1.0.0
                components:
                  securitySchemes:
                    bearerAuth:
                      $ref: components/bearerAuth.yml
                  schemas:
                    Existing:
                      type: object
                paths: {}
                """);
        write("components/bearerAuth.yml", """
                type: http
                scheme: bearer
                """);

        JsonNode root = merge().execute().node();

        assertThat(root.at("/components/securitySchemes/bearerAuth/type").asText()).isEqualTo("http");
        assertThat(root.at("/components/schemas/Existing/type").asText()).isEqualTo("object");
    }

    @Test
    void should_keep_missing_path_file_as_reference() throws IOException {
        write("main.yml", """
                openapi: 3.0.0
                info:
                  title: Test
                  version: 1.0.0
                paths:
                  /missing:
                    $ref: paths/missing.yml
                """);

        JsonNode root = merge().execute().node();

        assertThat(root.at("/paths/~1missing/$ref").asText()).isEqualTo("paths/missing.yml");
    }

    private MergeSwagger merge() {
        return new MergeSwaggerImpl(
                new GetSwaggerNodeJacksonFromFileImpl(tempDir.resolve("main.yml").toFile()),
                tempDir.toFile());
    }

    private void write(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
