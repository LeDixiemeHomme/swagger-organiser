package org.valle.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.fromnode.GetSwaggerNodeFromNodeImpl;
import org.valle.provide.fromfile.jackson.GetSwaggerNodeJacksonFromFileImpl;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.valle.utils.JacksonUtils.readValue;

class DecomposeSwaggerImplTest {

    private static final String SWAGGER_FILE_PATH_FORM = "src/test/resources/decomposed/swagger-initial.%s";

    @ParameterizedTest
    @ValueSource(strings = {"yml", "yaml", "json"})
    void test_execute_OK(String extension) {
        // Arrange
        File file = new File(SWAGGER_FILE_PATH_FORM.formatted(extension));
        DecomposeSwagger decomposeSwagger = new DecomposeSwaggerImpl(
                new GetSwaggerNodeJacksonFromFileImpl(file)
        );
        // Act
        DecomposedSwagger actual = decomposeSwagger.execute();
        // Assert
        File file1 = new File("src/test/resources/decomposed/test-res/main.%s".formatted(extension));
        assertThat(readValue(file1)).isEqualTo(actual.main().node());
        actual.paths().node().fields().forEachRemaining(field -> {
            File file2 = new File("src/test/resources/decomposed/test-res/paths/%s.%s".formatted(field.getKey(), extension));
            JsonNode expected = readValue(file2);
            assertThat(expected).isEqualTo(field.getValue());
        });
        actual.components().node().fields().forEachRemaining(field -> {
            File file2 = new File("src/test/resources/decomposed/test-res/components/%s.%s".formatted(field.getKey(), extension));
            JsonNode expected = readValue(file2);
            assertThat(expected).isEqualTo(field.getValue());
        });
    }

    @org.junit.jupiter.api.Test
    void should_preserve_component_categories_in_metadata() throws Exception {
        JsonNode input = new ObjectMapper().readTree("""
                {
                  "openapi": "3.0.0",
                  "info": {"title": "Test", "version": "1.0.0"},
                  "paths": {},
                  "components": {
                    "securitySchemes": {
                      "bearerAuth": {"type": "http", "scheme": "bearer"}
                    },
                    "responses": {
                      "NotFound": {"description": "Not found"}
                    },
                    "parameters": {
                      "TraceId": {"name": "X-Trace-Id", "in": "header", "schema": {"type": "string"}}
                    }
                  }
                }
                """);

        DecomposedSwagger result = new DecomposeSwaggerImpl(
                new GetSwaggerNodeFromNodeImpl(SwaggerNode.builder()
                        .node(input)
                        .extension(Extension.YML)
                        .build())
        ).execute();

        assertThat(result.componentCategories())
                .containsEntry("bearerAuth", "securitySchemes")
                .containsEntry("NotFound", "responses")
                .containsEntry("TraceId", "parameters");
        assertThat(result.components().node().fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("bearerAuth", "NotFound", "TraceId");
    }

    @org.junit.jupiter.api.Test
    void should_preserve_nested_path_comment_in_decomposed_zip() throws Exception {
        Path swaggerFile = Path.of("src/main/resources/q1-api.yml");
        String source = Files.readString(swaggerFile);
        SwaggerNode swaggerNode = org.valle.utils.JacksonUtils.getSwaggerNode(
                swaggerFile.toFile(), true);

        DecomposedSwagger result = new DecomposeSwaggerImpl(
                new GetSwaggerNodeFromNodeImpl(swaggerNode)).execute();

        String pathContent = null;
        try (ZipInputStream zip = new ZipInputStream(
                new ByteArrayInputStream(org.valle.utils.ZipUtils.build(result)))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("paths/entities-customer_uom_code-customers-customer_id-contracts-contract_id-acts-act_code-survey.yml"
                        .equals(entry.getName())) {
                    pathContent = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        assertThat(pathContent).isNotNull();
        assertThat(pathContent).contains("# todo test a delete");
        String targetSource = source.substring(source.indexOf(
                "/entities/{customer_uom_code}/customers/{customer_id}/contracts/{contract_id}/acts/{act_code}/survey:"));
        assertThat(targetSource.indexOf("# todo test a delete"))
                .isLessThan(targetSource.indexOf("get:"));
        assertThat(pathContent.indexOf("# todo test a delete"))
                .isLessThan(pathContent.indexOf("get:"));
    }
}
