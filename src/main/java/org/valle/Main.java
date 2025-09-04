package org.valle;

import lombok.extern.slf4j.Slf4j;
import org.valle.persist.jackson.PersistDecomposedSwaggerImpl;
import org.valle.present.logger.ShowEndpointsLoggerImpl;
import org.valle.process.ClearEndpointOnDemandImpl;
import org.valle.process.DecomposeSwaggerImpl;
import org.valle.process.ShowEndpointsImpl;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.fromnode.GetSwaggerNodeFromNodeImpl;
import org.valle.provide.jackson.GetSwaggerNodeJacksonImpl;
import org.valle.utils.JacksonUtils;

import java.io.File;
import java.util.Set;

@Slf4j
public class Main {

    public static final String COBAYE_PATH = "src/main/resources/swagger-cobaye.yml";

    public static void main(String[] args) {

        JacksonUtils jacksonUtilsCobaye = new JacksonUtils(new File(COBAYE_PATH));

        new ShowEndpointsImpl(new GetSwaggerNodeJacksonImpl(jacksonUtilsCobaye), new ShowEndpointsLoggerImpl()).execute();

        Set<EndPoint> endpointsToClean = Set.of(
                EndPoint.builder()
                        .method("post")
                        .path("/cadh/v1/operations")
                        .build()
        );

        SwaggerNode cleared = new ClearEndpointOnDemandImpl(new GetSwaggerNodeJacksonImpl(jacksonUtilsCobaye)).execute(endpointsToClean);

        new ShowEndpointsImpl(new GetSwaggerNodeFromNodeImpl(cleared), new ShowEndpointsLoggerImpl()).execute();

        DecomposedSwagger decomposed = new DecomposeSwaggerImpl(new GetSwaggerNodeJacksonImpl(jacksonUtilsCobaye)).execute();

        log.info("decoposed swagger: {}", decomposed);

        new PersistDecomposedSwaggerImpl("src/main/resources/gene-res").persist(decomposed);
    }
}