package org.valle;

import org.valle.present.logger.ShowEndpointsLoggerImpl;
import org.valle.process.ClearEndpointOnDemandImpl;
import org.valle.process.ShowEndpointsImpl;
import org.valle.process.models.EndPoint;
import org.valle.provide.jackson.GetAllEndpointsFromJackson;
import org.valle.provide.jackson.GetAllSchemasFromJacksonImpl;
import org.valle.provide.jackson.GetSwaggerRawValueJacksonImpl;
import org.valle.provide.jackson.GetSwaggerValueJacksonImpl;
import org.valle.provide.jackson.JacksonUtils;

import java.io.File;
import java.util.Set;

public class Main {
    public static void main(String[] args) {

        JacksonUtils jacksonUtilsCobaye = new JacksonUtils(new File("src/main/resources/swagger-cobaye.yml"));
        JacksonUtils jacksonUtilsCleaned = new JacksonUtils(new File("src/main/resources/swagger-cleaned.yml"));

        GetAllEndpointsFromJackson getAllEndpoints = new GetAllEndpointsFromJackson(jacksonUtilsCobaye);

        ShowEndpointsImpl showEndpointsCobaye = new ShowEndpointsImpl(
                getAllEndpoints,
                new ShowEndpointsLoggerImpl()
        );

        ShowEndpointsImpl showEndpointsCleaned = new ShowEndpointsImpl(
                new GetAllEndpointsFromJackson(jacksonUtilsCleaned),
                new ShowEndpointsLoggerImpl()
        );
        System.out.println(new GetAllSchemasFromJacksonImpl(jacksonUtilsCobaye).provide().size());
        showEndpointsCobaye.execute();

        ClearEndpointOnDemandImpl clearEndpointOnDemand = new ClearEndpointOnDemandImpl(
                getAllEndpoints,
                new GetSwaggerRawValueJacksonImpl(jacksonUtilsCobaye),
                new GetSwaggerValueJacksonImpl(jacksonUtilsCobaye)
        );

        // demander quels endpoints à supprimer via la console
        Set<EndPoint> endpointsToClean = Set.of(
                EndPoint.builder()
                        .method("post")
                        .path("/cadh/v1/operations")
                        .build()
        );

        System.out.println(new GetAllSchemasFromJacksonImpl(jacksonUtilsCleaned).provide().size());
        showEndpointsCleaned.execute();
        clearEndpointOnDemand.execute(endpointsToClean);
        System.out.println(new GetAllSchemasFromJacksonImpl(jacksonUtilsCleaned).provide().size());
        showEndpointsCleaned.execute();
    }
}