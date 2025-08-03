package org.valle;

import org.valle.persist.jackson.PersistResultFileWithJacksonImpl;
import org.valle.present.logger.ShowEndpointsLoggerImpl;
import org.valle.process.ClearEndpointOnDemandImpl;
import org.valle.process.ShowEndpointsImpl;
import org.valle.process.models.EndPoint;
import org.valle.provide.jackson.GetAllComponentsFromJacksonImpl;
import org.valle.provide.jackson.GetAllEndpointsFromJackson;
import org.valle.provide.jackson.GetAllPathsFromJacksonImpl;
import org.valle.provide.jackson.GetAllSchemasFromJacksonImpl;

import java.util.Set;

public class Main {
    public static void main(String[] args) {
        GetAllEndpointsFromJackson getAllEndpoints = new GetAllEndpointsFromJackson("src/main/resources/swagger-cobaye.yml");
        ShowEndpointsImpl process1 = new ShowEndpointsImpl(
                getAllEndpoints,
                new ShowEndpointsLoggerImpl()
        );

        process1.execute();

        ClearEndpointOnDemandImpl clearEndpointOnDemand = new ClearEndpointOnDemandImpl(
                getAllEndpoints,
                new GetAllPathsFromJacksonImpl("src/main/resources/swagger-cobaye.yml"),
                new GetAllComponentsFromJacksonImpl("src/main/resources/swagger-cobaye.yml"),
                new PersistResultFileWithJacksonImpl("src/main/resources/swagger-cleaned.yml")
        );

        // demander quels endpoints à supprimer via la console
        Set<EndPoint> endpointsToClean = Set.of(
                EndPoint.builder()
                        .method("post")
                        .path("/cadh/v1/operations")
                        .build()
        );

        clearEndpointOnDemand.execute(endpointsToClean);

        ShowEndpointsImpl process2 = new ShowEndpointsImpl(
                new GetAllEndpointsFromJackson("src/main/resources/swagger-cleaned.yml"),
                new ShowEndpointsLoggerImpl()
        );
        process2.execute();
    }
}