package org.valle.provide.jackson;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class GetAllEndpointsFromJacksonTest {

    public static final String SWAGGER_FILE_PATH = "src/test/resources/swagger-cobaye.yml";

    @Test
    void provide() {
        // Arrange
        JacksonUtils jacksonUtilsCobaye = new JacksonUtils(new File(SWAGGER_FILE_PATH));
        GetAllEndpointsFromJackson getAllPathsFromJackson = new GetAllEndpointsFromJackson(jacksonUtilsCobaye);
        // Act
        var endpoints = getAllPathsFromJackson.provide();
        // Assert
        assertThat(endpoints).hasSize(5);
    }
}