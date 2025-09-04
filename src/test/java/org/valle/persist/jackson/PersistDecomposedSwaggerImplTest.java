package org.valle.persist.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.valle.process.DecomposeSwagger;
import org.valle.process.DecomposeSwaggerImpl;
import org.valle.process.models.DecomposedSwagger;
import org.valle.provide.fromfile.jackson.GetSwaggerNodeJacksonFromFileImpl;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.valle.utils.JacksonUtils.readValue;

class PersistDecomposedSwaggerImplTest {

    private static final String SWAGGER_FILE_PATH_FORM = "src/test/resources/decomposed/swagger-initial.%s";

    @ParameterizedTest
    @ValueSource(strings = {"yml", "yaml", "json"})
    void test_persist(String extension) {
        // Arrange
        File file = new File(SWAGGER_FILE_PATH_FORM.formatted(extension));
        DecomposeSwagger decomposeSwagger = new DecomposeSwaggerImpl(
                new GetSwaggerNodeJacksonFromFileImpl(file)
        );
        DecomposedSwagger decomposedSwagger = decomposeSwagger.execute();
        PersistDecomposedSwaggerImpl persistDecomposedSwagger = new PersistDecomposedSwaggerImpl("src/test/resources/decomposed/persit-test-res");
        // Act
        persistDecomposedSwagger.persist(decomposedSwagger);
        // Assert
        File file1 = new File("src/test/resources/decomposed/persit-test-res/main.%s".formatted(extension));
        assertThat(readValue(file1)).isEqualTo(decomposedSwagger.main().node());
        decomposedSwagger.paths().node().fields().forEachRemaining(field -> {
            File file2 = new File("src/test/resources/decomposed/persit-test-res/paths/%s.%s".formatted(field.getKey(), extension));
            JsonNode expected = readValue(file2);
            assertThat(expected).isEqualTo(field.getValue());
        });
        decomposedSwagger.components().node().fields().forEachRemaining(field -> {
            File file2 = new File("src/test/resources/decomposed/persit-test-res/components/%s.%s".formatted(field.getKey(), extension));
            JsonNode expected = readValue(file2);
            assertThat(expected).isEqualTo(field.getValue());
        });
    }
}