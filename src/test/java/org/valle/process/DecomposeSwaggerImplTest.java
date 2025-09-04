package org.valle.process;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.valle.process.models.DecomposedSwagger;
import org.valle.provide.jackson.GetSwaggerNodeJacksonImpl;
import org.valle.provide.jackson.JacksonUtils;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class DecomposeSwaggerImplTest {

    private static final String SWAGGER_FILE_PATH_FORM = "src/test/resources/decomposed/swagger-initial.%s";

    @ParameterizedTest
    @ValueSource(strings = {"yml", "yaml", "json"})
    void test_execute_OK(String extension) {
        // Arrange
        JacksonUtils jacksonUtilsInitial = new JacksonUtils(new File(SWAGGER_FILE_PATH_FORM.formatted(extension)));

        DecomposeSwagger decomposeSwagger = new DecomposeSwaggerImpl(
                new GetSwaggerNodeJacksonImpl(jacksonUtilsInitial)
        );
        // Act
        DecomposedSwagger actual = decomposeSwagger.execute();
        // Assert
        assertThat(new JacksonUtils(new File("src/test/resources/decomposed/test-res/main.%s".formatted(extension))).readValue()).isEqualTo(actual.main().node());
        actual.paths().node().fields().forEachRemaining(field -> {
            JsonNode expected = new JacksonUtils(new File("src/test/resources/decomposed/test-res/paths/%s.%s".formatted(field.getKey(), extension))).readValue();
            assertThat(expected).isEqualTo(field.getValue());
        });
        actual.components().node().fields().forEachRemaining(field -> {
            JsonNode expected = new JacksonUtils(new File("src/test/resources/decomposed/test-res/components/%s.%s".formatted(field.getKey(), extension))).readValue();
            assertThat(expected).isEqualTo(field.getValue());
        });
    }
}