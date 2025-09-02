package org.valle.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.valle.persist.PersistResult;
import org.valle.persist.PersistResultNodeImpl;
import org.valle.persist.jackson.PersistResultFileWithJacksonImpl;
import org.valle.provide.jackson.GetSwaggerNodeJacksonImpl;
import org.valle.provide.jackson.GetSwaggerRawValueJacksonImpl;
import org.valle.provide.jackson.JacksonUtils;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

class DecomposeSwaggerImplTest {

    private static final String SWAGGER_FILE_PATH = "src/test/resources/decomposed/swagger-initial.yml";

    private final JacksonUtils jacksonUtilsInitial = new JacksonUtils(new File(SWAGGER_FILE_PATH));

    private final DecomposeSwagger decomposeSwagger = new DecomposeSwaggerImpl(
            new GetSwaggerRawValueJacksonImpl(jacksonUtilsInitial),
            new GetSwaggerNodeJacksonImpl(jacksonUtilsInitial)
    );

    @Test
    void test_execute_OK() {
        // Arrange
        // Act
        Map<String, Object> actual = decomposeSwagger.execute();
        // Assert
        Map<String, Object> paths = (Map<String, Object>) actual.get("paths");

        paths.entrySet().forEach(entry -> {
            JsonNode node = ((JsonNode) entry.getValue());
            // create dir src/test/resources/decomposed/test-res/paths
            File pathsDir = new File("src/test/resources/decomposed/test-res/paths");
            if (pathsDir.exists()) {
                pathsDir.delete();
            }
            pathsDir.mkdirs();
            JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File("src/test/resources/decomposed/test-res/paths/%s.yaml".formatted(entry.getKey())));
            new PersistResultNodeImpl(jacksonUtilsTmp).persist((ObjectNode) node);
        });

        Map<String, Object> components = (Map<String, Object>) actual.get("components");

        components.entrySet().forEach(entry -> {
            JsonNode node = ((JsonNode) entry.getValue());
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

            File pathsDir = new File("src/test/resources/decomposed/test-res/components");
            if (pathsDir.exists()) {
                pathsDir.delete();
            }
            pathsDir.mkdirs();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File("src/test/resources/decomposed/test-res/components/%s.yaml".formatted(field.getKey())));
                new PersistResultNodeImpl(jacksonUtilsTmp).persist((ObjectNode) field.getValue());
            }
        });

        Map<String, Object> main = (Map<String, Object>) actual.get("main");
        JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File("src/test/resources/decomposed/test-res/main.yaml"));
        new PersistResultFileWithJacksonImpl(jacksonUtilsTmp).persist(main);
    }
}