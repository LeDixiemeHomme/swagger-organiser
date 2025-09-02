package org.valle.process.models;

import org.junit.jupiter.api.Test;
import org.valle.provide.jackson.JacksonUtils;

import java.io.File;
import java.util.Map;

class SwaggerRawValueTest {

    File swaggerFile = new File("src/test/resources/decomposed/swagger-initial.yml");
    JacksonUtils jacksonUtils = new JacksonUtils(swaggerFile);
    Map<String, Object> rawValue = jacksonUtils.readRawValue();
    SwaggerRawValue swaggerRawValue = new SwaggerRawValue(rawValue, jacksonUtils.getSwaggerFileExtension());

    @Test
    void test_changePathReferences() {
        // Arrange
        // Act
        SwaggerRawValue actual = swaggerRawValue.changePathReferences();
        // Assert
        Map<String, Object> paths = (Map<String, Object>) actual.rawValue().get("paths");
        paths.entrySet().forEach(entry -> {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        });
    }

    @Test
    void test_changeComponentsReferences() {
        // Arrange
        // Act
        SwaggerRawValue actual = swaggerRawValue.changeComponentsReferences();
        // Assert
        Map<String, Object> components = (Map<String, Object>) actual.rawValue().get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        schemas.entrySet().forEach(entry -> {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        });
    }
}