package org.valle.provide.jackson;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GetAllEndpointsFromJacksonTest {

    public static final String SWAGGER_FILE_PATH = "src/test/resources/swagger-cobaye.yml";

    @Test
    void provide() {
        // Arrange
        GetAllEndpointsFromJackson getAllPathsFromJackson = new GetAllEndpointsFromJackson(SWAGGER_FILE_PATH);
        // Act
        var endpoints = getAllPathsFromJackson.provide();
        // Assert
        assertThat(endpoints).hasSize(5);
    }
}