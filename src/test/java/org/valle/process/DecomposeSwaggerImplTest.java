package org.valle.process;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.valle.persist.PersistResultNodeImpl;
import org.valle.process.models.DecomposedSwagger;
import org.valle.provide.jackson.GetSwaggerNodeJacksonImpl;
import org.valle.provide.jackson.JacksonUtils;

import java.io.File;

class DecomposeSwaggerImplTest {

    private static final String SWAGGER_FILE_PATH = "src/test/resources/decomposed/swagger-initial.yml";

    private final JacksonUtils jacksonUtilsInitial = new JacksonUtils(new File(SWAGGER_FILE_PATH));

    private final DecomposeSwagger decomposeSwagger = new DecomposeSwaggerImpl(
            new GetSwaggerNodeJacksonImpl(jacksonUtilsInitial)
    );

    @Test
    void test_execute_OK() {
        // Arrange
        // Act
        DecomposedSwagger actual = decomposeSwagger.execute();
        // Assert
        actual.paths().node().fields().forEachRemaining(entry -> {
            // create dir src/test/resources/decomposed/test-res/paths
            File pathsDir = new File("src/test/resources/decomposed/test-res/paths");
            if (pathsDir.exists()) {
                pathsDir.delete();
            }
            pathsDir.mkdirs();
            JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File("src/test/resources/decomposed/test-res/paths/%s.yaml".formatted(entry.getKey())));
            new PersistResultNodeImpl(jacksonUtilsTmp).persist((ObjectNode) entry.getValue());
        });

        actual.components().node().fields().forEachRemaining(entry -> {
            File pathsDir = new File("src/test/resources/decomposed/test-res/components");
            if (pathsDir.exists()) {
                pathsDir.delete();
            }
            pathsDir.mkdirs();
            JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File("src/test/resources/decomposed/test-res/components/%s.yaml".formatted(entry.getKey())));
            new PersistResultNodeImpl(jacksonUtilsTmp).persist((ObjectNode) entry.getValue());
        });

        JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File("src/test/resources/decomposed/test-res/main.yaml"));
        new PersistResultNodeImpl(jacksonUtilsTmp).persist((ObjectNode) actual.main().node());
    }
}