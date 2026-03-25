package org.valle.process;

import org.junit.jupiter.api.Test;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.fromfile.jackson.GetSwaggerNodeJacksonFromFileImpl;

import java.io.File;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KeepEndpointOnDemandImplTest {

    static final String SWAGGER_PATH = "src/main/resources/swagger-cobaye.yml";

    KeepEndpointOnDemandImpl keepEndpointOnDemand = new KeepEndpointOnDemandImpl(
            new GetSwaggerNodeJacksonFromFileImpl(new File(SWAGGER_PATH))
    );

    @Test
    void should_keep_only_specified_endpoint_and_remove_others() {
        // Arrange : on ne conserve que POST /profiling
        Set<EndPoint> endpointsToKeep = Set.of(
                EndPoint.builder()
                        .method("post")
                        .path("/profiling")
                        .build()
        );

        // Act
        SwaggerNode actual = keepEndpointOnDemand.execute(endpointsToKeep);

        // Assert : un seul path conservé, avec une seule méthode
        assertThat(actual.node().get("paths").size()).isEqualTo(1);
        assertThat(actual.node().get("paths").has("/profiling")).isTrue();
        assertThat(actual.node().get("paths").get("/profiling").has("post")).isTrue();
    }

    @Test
    void should_keep_multiple_endpoints() {
        // Arrange : on conserve tous les endpoints existants → résultat = swagger inchangé
        SwaggerNode original = new GetSwaggerNodeJacksonFromFileImpl(new File(SWAGGER_PATH)).provide();
        int totalPaths = original.node().get("paths").size();
        Set<EndPoint> allEndpoints = original.getAllEndpoints();

        KeepEndpointOnDemandImpl keepAll = new KeepEndpointOnDemandImpl(
                new GetSwaggerNodeJacksonFromFileImpl(new File(SWAGGER_PATH))
        );
        SwaggerNode actual = keepAll.execute(allEndpoints);

        // Tous les paths sont conservés
        assertThat(actual.node().get("paths").size()).isEqualTo(totalPaths);
    }

    @Test
    void should_remove_schemas_exclusively_used_by_removed_endpoints() {
        // On garde seulement POST /profiling
        // Les schémas utilisés uniquement par les autres endpoints doivent disparaître
        Set<EndPoint> endpointsToKeep = Set.of(
                EndPoint.builder()
                        .method("post")
                        .path("/profiling")
                        .build()
        );

        SwaggerNode original = new GetSwaggerNodeJacksonFromFileImpl(new File(SWAGGER_PATH)).provide();
        int totalSchemas = original.node().get("components").get("schemas").size();

        SwaggerNode actual = keepEndpointOnDemand.execute(endpointsToKeep);

        // Les composants ne doivent contenir que les schémas référencés par POST /profiling
        assertThat(actual.node().get("components").get("schemas").size())
                .isLessThan(totalSchemas);
    }

    @Test
    void should_remove_all_endpoints_except_kept_one() {
        // On garde GET /surveys, tous les autres paths doivent disparaître
        Set<EndPoint> endpointsToKeep = Set.of(
                EndPoint.builder()
                        .method("get")
                        .path("/surveys")
                        .build()
        );

        KeepEndpointOnDemandImpl keepSurveys = new KeepEndpointOnDemandImpl(
                new GetSwaggerNodeJacksonFromFileImpl(new File(SWAGGER_PATH))
        );
        SwaggerNode actual = keepSurveys.execute(endpointsToKeep);

        assertThat(actual.node().get("paths").size()).isEqualTo(1);
        assertThat(actual.node().get("paths").has("/surveys")).isTrue();
        assertThat(actual.node().get("paths").get("/surveys").has("get")).isTrue();
    }
}


