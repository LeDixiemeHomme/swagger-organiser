package org.valle.provide.jackson;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GetAllSchemasFromJacksonImplTest {

    public static final String SWAGGER_FILE_PATH = "src/test/resources/swagger-cobaye.yml";

    @Test
    void provide() {
        // Arrange
        GetAllSchemasFromJacksonImpl getAllSchemasFromJackson = new GetAllSchemasFromJacksonImpl(SWAGGER_FILE_PATH);
        // Act
        var schemas = getAllSchemasFromJackson.provide();
        // Assert
        assertThat(schemas).hasSize(29);
//        for (Map.Entry<String, Object> entry : schemas.entrySet()) {
//            System.out.println("Schema: " + entry.getKey());
//            System.out.println("Details: " + entry.getValue());
//        }
    }
}