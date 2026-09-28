package org.valle.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ZipUtilsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void should_build_zip_with_single_node() throws IOException {
        SwaggerNode node = node("title", "Test");

        List<String> entries = entries(ZipUtils.buildFromNode(node, "swagger.yml"));

        assertThat(entries).containsExactly("swagger.yml");
    }

    @Test
    void should_build_decomposed_zip_with_main_paths_and_components() throws IOException {
        DecomposedSwagger decomposed = DecomposedSwagger.builder()
                .main(node("title", "Main"))
                .paths(node("users", "get: {}"))
                .components(node("User", "type: object"))
                .build();

        List<String> entries = entries(ZipUtils.build(decomposed));

        assertThat(entries).containsExactly("main.yml", "paths/users.yml", "components/User.yml");
    }

    @Test
    void should_include_component_category_metadata_in_decomposed_zip() throws IOException {
        DecomposedSwagger decomposed = DecomposedSwagger.builder()
                .main(node("title", "Main"))
                .components(node("TraceId", "name: X-Trace-Id"))
                .componentCategories(Map.of("TraceId", "parameters"))
                .build();

        List<String> entries = entries(ZipUtils.build(decomposed));

        assertThat(entries).containsExactly(
                "main.yml",
                "components/TraceId.yml",
                DecomposedSwagger.COMPONENT_CATEGORIES_FILE);
    }

    private SwaggerNode node(String field, String value) {
        ObjectNode object = mapper.createObjectNode();
        object.put(field, value);
        return SwaggerNode.builder().node(object).extension(Extension.YML).build();
    }

    private List<String> entries(byte[] zip) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }
}
