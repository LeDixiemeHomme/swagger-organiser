package org.valle.persist.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.valle.process.DecomposeSwagger;
import org.valle.process.DecomposeSwaggerImpl;
import org.valle.process.models.DecomposedSwagger;
import org.valle.provide.jackson.GetSwaggerNodeJacksonImpl;
import org.valle.utils.JacksonUtils;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class PersistDecomposedSwaggerImplTest {

    private static final String SWAGGER_FILE_PATH_FORM = "src/test/resources/decomposed/swagger-initial.%s";

    @ParameterizedTest
    @ValueSource(strings = {"yml", "yaml", "json"})
    void test_persist(String extension) {
        // Arrange
        JacksonUtils jacksonUtilsInitial = new JacksonUtils(new File(SWAGGER_FILE_PATH_FORM.formatted(extension)));

        DecomposeSwagger decomposeSwagger = new DecomposeSwaggerImpl(
                new GetSwaggerNodeJacksonImpl(jacksonUtilsInitial)
        );
        DecomposedSwagger decomposedSwagger = decomposeSwagger.execute();
        PersistDecomposedSwaggerImpl persistDecomposedSwagger = new PersistDecomposedSwaggerImpl("src/test/resources/decomposed/persit-test-res");
        // Act
        persistDecomposedSwagger.persist(decomposedSwagger);
        // Assert
        assertThat(new JacksonUtils(new File("src/test/resources/decomposed/persit-test-res/main.%s".formatted(extension))).readValue()).isEqualTo(decomposedSwagger.main().node());
        decomposedSwagger.paths().node().fields().forEachRemaining(field -> {
            JsonNode expected = new JacksonUtils(new File("src/test/resources/decomposed/persit-test-res/paths/%s.%s".formatted(field.getKey(), extension))).readValue();
            assertThat(expected).isEqualTo(field.getValue());
        });
        decomposedSwagger.components().node().fields().forEachRemaining(field -> {
            JsonNode expected = new JacksonUtils(new File("src/test/resources/decomposed/persit-test-res/components/%s.%s".formatted(field.getKey(), extension))).readValue();
            assertThat(expected).isEqualTo(field.getValue());
        });
    }
}