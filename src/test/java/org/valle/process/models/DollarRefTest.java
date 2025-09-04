package org.valle.process.models;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.valle.utils.JacksonUtils.readValue;

class DollarRefTest {

    @Test
    void test_getReferencedName() {
        // Arrange
        String rawValue = "#/components/schemas/OperationInputDTOV1";
        DollarRef dollarRef = new DollarRef(rawValue);
        // Act
        String actual = dollarRef.getReferencedName();
        // Assert
        assertThat(actual).isEqualTo("OperationInputDTOV1");
    }

    @Test
    void test_getComponentFileReference() {
        // Arrange
        String rawValue = "#/components/schemas/OperationInputDTOV1";
        DollarRef dollarRef = new DollarRef(rawValue);
        // Act
        String actual = dollarRef.getComponentFileReference();
        // Assert
        assertThat(actual).isEqualTo("../components/OperationInputDTOV1");
    }

    @Test
    void test_getReferencedNode() {
        // Arrange
        String rawValue = "#/components/schemas/OperationInputDTOV1";
        DollarRef dollarRef = new DollarRef(rawValue);

        File file = new File("src/test/resources/cleared/swagger-cobaye.yml");

        JsonNode objectMap = readValue(file);
        // Act
        JsonNode actual = dollarRef.getReferencedNode(objectMap);
        // Assert
        assertThat(actual.get("title").asText()).isEqualTo("OperationInputDTOV1");
    }
}