package org.valle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.valle.persist.PersistResultNodeImpl;
import org.valle.persist.jackson.PersistResultFileWithJacksonImpl;
import org.valle.present.logger.ShowEndpointsLoggerImpl;
import org.valle.process.ClearEndpointOnDemandImpl;
import org.valle.process.DecomposeSwaggerImpl;
import org.valle.process.ShowEndpointsImpl;
import org.valle.process.models.EndPoint;
import org.valle.provide.jackson.GetAllEndpointsFromJackson;
import org.valle.provide.jackson.GetAllSchemasFromJacksonImpl;
import org.valle.provide.jackson.GetSwaggerNodeJacksonImpl;
import org.valle.provide.jackson.GetSwaggerRawValueJacksonImpl;
import org.valle.provide.jackson.JacksonUtils;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Slf4j
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
                new GetSwaggerNodeJacksonImpl(jacksonUtilsCobaye)
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

        JacksonUtils jacksonUtilsInitial = new JacksonUtils(new File("src/main/resources/swagger-cobaye.yml"));

        DecomposeSwaggerImpl decomposeSwagger = new DecomposeSwaggerImpl(
                new GetSwaggerRawValueJacksonImpl(jacksonUtilsInitial),
                new GetSwaggerNodeJacksonImpl(jacksonUtilsInitial)
        );

        Map<String, Object> decomposed = decomposeSwagger.execute();
        log.info("decoposed swagger: {}", decomposed);

        Map<String, Object> paths = (Map<String, Object>) decomposed.get("paths");

        paths.entrySet().forEach(entry -> {
            JsonNode node = ((JsonNode) entry.getValue());
            // create dir src/main/resources/paths
            File pathsDir = new File("src/main/resources/paths");
            if (pathsDir.exists()) {
                pathsDir.delete();
            }
            pathsDir.mkdirs();
            JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File("src/main/resources/paths/%s.yaml".formatted(entry.getKey())));
            new PersistResultNodeImpl(jacksonUtilsTmp).persist((ObjectNode) node);
        });

        Map<String, Object> components = (Map<String, Object>) decomposed.get("components");

        components.entrySet().forEach(entry -> {
            JsonNode node = ((JsonNode) entry.getValue());
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

            File pathsDir = new File("src/main/resources/components");
            if (pathsDir.exists()) {
                pathsDir.delete();
            }
            pathsDir.mkdirs();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File("src/main/resources/components/%s.yaml".formatted(field.getKey())));
                new PersistResultNodeImpl(jacksonUtilsTmp).persist((ObjectNode) field.getValue());
            }
        });

        Map<String, Object> main = (Map<String, Object>) decomposed.get("main");
        JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File("src/main/resources/main.yaml"));
        new PersistResultFileWithJacksonImpl(jacksonUtilsTmp).persist(main);
    }
}