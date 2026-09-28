package org.valle.persist.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PersistResultNodeImplTest {

    @TempDir
    Path tempDir;

    @Test
    void should_persist_json_nodes_to_the_requested_file() throws IOException {
        Path target = tempDir.resolve("result.json");
        ObjectNode node = new ObjectMapper().createObjectNode().put("openapi", "3.0.0");

        new PersistResultNodeImpl(target.toFile()).persist(node);

        assertThat(new ObjectMapper().readTree(target.toFile())).isEqualTo(node);
    }
}
