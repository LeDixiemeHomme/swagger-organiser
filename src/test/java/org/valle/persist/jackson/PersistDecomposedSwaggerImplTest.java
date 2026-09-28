package org.valle.persist.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import org.valle.process.DecomposeSwagger;
import org.valle.process.DecomposeSwaggerImpl;
import org.valle.process.models.DecomposedSwagger;
import org.valle.provide.fromfile.jackson.GetSwaggerNodeJacksonFromFileImpl;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.valle.utils.JacksonUtils.readValue;

class PersistDecomposedSwaggerImplTest {

    private static final String SWAGGER_FILE_PATH_FORM = "src/test/resources/decomposed/swagger-initial.%s";

    @TempDir
    Path outputDirectory;

    @ParameterizedTest
    @ValueSource(strings = {"yml", "yaml", "json"})
    void test_persist(String extension) {
        // Arrange
        File file = new File(SWAGGER_FILE_PATH_FORM.formatted(extension));
        DecomposeSwagger decomposeSwagger = new DecomposeSwaggerImpl(
                new GetSwaggerNodeJacksonFromFileImpl(file)
        );
        DecomposedSwagger decomposedSwagger = decomposeSwagger.execute();
        PersistDecomposedSwaggerImpl persistDecomposedSwagger =
                new PersistDecomposedSwaggerImpl(outputDirectory.toString());
        // Act
        persistDecomposedSwagger.persist(decomposedSwagger);
        // Assert
        File file1 = outputDirectory.resolve("main.%s".formatted(extension)).toFile();
        assertThat(readValue(file1)).isEqualTo(decomposedSwagger.main().node());
        decomposedSwagger.paths().node().fields().forEachRemaining(field -> {
            File file2 = outputDirectory.resolve("paths/%s.%s".formatted(field.getKey(), extension)).toFile();
            JsonNode expected = readValue(file2);
            assertThat(expected).isEqualTo(field.getValue());
        });
        decomposedSwagger.components().node().fields().forEachRemaining(field -> {
            File file2 = outputDirectory.resolve("components/%s.%s".formatted(field.getKey(), extension)).toFile();
            JsonNode expected = readValue(file2);
            assertThat(expected).isEqualTo(field.getValue());
        });
    }

    @org.junit.jupiter.api.Test
    void should_reject_component_names_escaping_output_directory() {
        DecomposedSwagger decomposed = DecomposedSwagger.builder()
                .main(org.valle.process.models.SwaggerNode.builder()
                        .node(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode())
                        .extension(org.valle.process.models.Extension.JSON)
                        .build())
                .components(org.valle.process.models.SwaggerNode.builder()
                        .node(new com.fasterxml.jackson.databind.ObjectMapper()
                                .createObjectNode()
                                .set("..\\outside", new com.fasterxml.jackson.databind.ObjectMapper()
                                        .createObjectNode()))
                        .extension(org.valle.process.models.Extension.JSON)
                        .build())
                .build();

        assertThatThrownBy(() -> new PersistDecomposedSwaggerImpl(outputDirectory.toString())
                .persist(decomposed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sort du répertoire autorisé");
    }
}