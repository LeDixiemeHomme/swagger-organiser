package org.valle;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.valle.persist.jackson.PersistResultNodeImpl;
import org.valle.present.logger.ShowEndpointsLoggerImpl;
import org.valle.process.ClearEndpointOnDemandImpl;
import org.valle.process.DecomposeSwaggerImpl;
import org.valle.process.ShowEndpointsImpl;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.EndPoint;
import org.valle.provide.jackson.GetSwaggerNodeJacksonImpl;
import org.valle.provide.jackson.JacksonUtils;

import java.io.File;
import java.util.Set;

@Slf4j
public class Main {
    public static void main(String[] args) {

        JacksonUtils jacksonUtilsCobaye = new JacksonUtils(new File("src/main/resources/swagger-cobaye.yml"));

        ShowEndpointsImpl showEndpointsCobaye = new ShowEndpointsImpl(
                new GetSwaggerNodeJacksonImpl(jacksonUtilsCobaye),
                new ShowEndpointsLoggerImpl()
        );

        showEndpointsCobaye.execute();

        ClearEndpointOnDemandImpl clearEndpointOnDemand = new ClearEndpointOnDemandImpl(
                new GetSwaggerNodeJacksonImpl(jacksonUtilsCobaye)
        );

        // demander quels endpoints à supprimer via la console
        Set<EndPoint> endpointsToClean = Set.of(
                EndPoint.builder()
                        .method("post")
                        .path("/cadh/v1/operations")
                        .build()
        );

        clearEndpointOnDemand.execute(endpointsToClean);

        JacksonUtils jacksonUtilsInitial = new JacksonUtils(new File("src/main/resources/swagger-cobaye.yml"));

        DecomposeSwaggerImpl decomposeSwagger = new DecomposeSwaggerImpl(
                new GetSwaggerNodeJacksonImpl(jacksonUtilsInitial)
        );

        DecomposedSwagger decomposed = decomposeSwagger.execute();
        log.info("decoposed swagger: {}", decomposed);

        decomposed.paths().node().fields().forEachRemaining(entry -> {
            // create dir src/main/resources/paths
            File pathsDir = new File("src/main/resources/paths");
            if (pathsDir.exists()) {
                pathsDir.delete();
            }
            pathsDir.mkdirs();
            JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File("src/main/resources/paths/%s.yaml".formatted(entry.getKey())));
            new PersistResultNodeImpl(jacksonUtilsTmp).persist((ObjectNode) entry.getValue());
        });

        decomposed.components().node().fields().forEachRemaining(entry -> {
            File pathsDir = new File("src/main/resources/components");
            if (pathsDir.exists()) {
                pathsDir.delete();
            }
            pathsDir.mkdirs();
            JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File("src/main/resources/components/%s.yaml".formatted(entry.getKey())));
            new PersistResultNodeImpl(jacksonUtilsTmp).persist((ObjectNode) entry.getValue());
        });

        JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File("src/main/resources/main.yaml"));
        new PersistResultNodeImpl(jacksonUtilsTmp).persist((ObjectNode) decomposed.main().node());
    }
}