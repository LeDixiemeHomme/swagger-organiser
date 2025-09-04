package org.valle.process;

import org.junit.jupiter.api.Test;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.jackson.GetSwaggerNodeJacksonImpl;
import org.valle.utils.JacksonUtils;

import java.io.File;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClearEndpointOnDemandImplTest {
    JacksonUtils jacksonUtilsCobaye = new JacksonUtils(new File("src/main/resources/swagger-cobaye.yml"));

    ClearEndpointOnDemandImpl clearEndpointOnDemand = new ClearEndpointOnDemandImpl(
            new GetSwaggerNodeJacksonImpl(jacksonUtilsCobaye)
    );

    @Test
    void test_execute() {
        // Arrange
        Set<EndPoint> endpointsToClean = Set.of(
                EndPoint.builder()
                        .method("post")
                        .path("/cadh/v1/operations")
                        .build()
        );
        // Act
        SwaggerNode actual = clearEndpointOnDemand.execute(endpointsToClean);
        // Arrange
        assertThat(actual.node().get("components").get("schemas").size()).isEqualTo(6);
        assertThat(actual.node().get("paths").size()).isEqualTo(4);
    }
}