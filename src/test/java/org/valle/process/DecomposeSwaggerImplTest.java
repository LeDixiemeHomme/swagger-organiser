package org.valle.process;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.valle.process.models.DecomposedSwagger;
import org.valle.provide.fromfile.jackson.GetSwaggerNodeJacksonFromFileImpl;

import java.io.File;

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
}