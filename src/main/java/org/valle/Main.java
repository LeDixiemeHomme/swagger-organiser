package org.valle;

import com.fasterxml.jackson.databind.JsonNode;
import org.valle.present.logger.ShowEndpointsLoggerImpl;
import org.valle.process.ShowEndpointsImpl;
import org.valle.process.models.EndPoint;
import org.valle.process.models.OpenApiObjects;
import org.valle.provide.jackson.GetAllEndpointsFromJackson;
import org.valle.provide.jackson.JacksonUtils;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        GetAllEndpointsFromJackson getAllEndpoints = new GetAllEndpointsFromJackson("src/main/resources/swagger-cobaye.yml");
        ShowEndpointsImpl process1 = new ShowEndpointsImpl(
                getAllEndpoints,
                new ShowEndpointsLoggerImpl()
        );

        process1.execute();

        // demander quelques endpoints à supprimer via la console
        Set<EndPoint> endpointsToClean = Set.of(EndPoint.builder()
                .method("post")
                .path("/cadh/v1/operations")
                .build());

        JacksonUtils jacksonUtils = new JacksonUtils(new File("src/main/resources/swagger-cobaye.yml"));

        JsonNode jsonNode = jacksonUtils.readValue();
        Map<String, Object> rawValue = jacksonUtils.readRawValue();

        OpenApiObjects openApiObjects = new OpenApiObjects(jsonNode, rawValue);

        Set<String> schemasToRemove = openApiObjects.getSchemaNamesToBeRemoved(
                getAllEndpoints.provide(),
                endpointsToClean
        );

        Map<String, Object> toWrite = openApiObjects.removeElementsByName(endpointsToClean, schemasToRemove);

        jacksonUtils.writeRawValue(toWrite, new File("src/main/resources/swagger-cleaned.yml"));

        ShowEndpointsImpl process2 = new ShowEndpointsImpl(
                new GetAllEndpointsFromJackson("src/main/resources/swagger-cleaned.yml"),
                new ShowEndpointsLoggerImpl()
        );
        process2.execute();
    }
}