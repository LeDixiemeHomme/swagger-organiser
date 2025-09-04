package org.valle;

import lombok.extern.slf4j.Slf4j;
import org.valle.persist.PersistDecomposedSwagger;
import org.valle.persist.jackson.PersistDecomposedSwaggerImpl;
import org.valle.present.logger.ShowEndpointsLoggerImpl;
import org.valle.process.ClearEndpointOnDemandImpl;
import org.valle.process.DecomposeSwaggerImpl;
import org.valle.process.ShowEndpointsImpl;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;
import org.valle.provide.fromnode.GetSwaggerNodeFromNodeImpl;
import org.valle.provide.fromfile.jackson.GetSwaggerNodeJacksonFromFileImpl;

import java.io.File;
import java.util.Set;

@Slf4j
public class Main {

    public static final String COBAYE_PATH = "src/main/resources/swagger-cobaye.yml";

    public static void main(String[] args) {
        EndPoint endPoint = EndPoint.builder()
                .method("post")
                .path("/cadh/v1/operations")
                .build();
        clear(endPoint);
        decompose();
    }

    public static void clear(EndPoint endPoint) {
        GetSwaggerNode getSwaggerNode = new GetSwaggerNodeJacksonFromFileImpl(new File(COBAYE_PATH));

        new ShowEndpointsImpl(getSwaggerNode, new ShowEndpointsLoggerImpl()).execute();

        SwaggerNode cleared = new ClearEndpointOnDemandImpl(getSwaggerNode)
                .execute(Set.of(endPoint));

        new ShowEndpointsImpl(new GetSwaggerNodeFromNodeImpl(cleared), new ShowEndpointsLoggerImpl())
                .execute();
    }

    public static void decompose() {
        GetSwaggerNode getSwaggerNode = new GetSwaggerNodeJacksonFromFileImpl(new File(COBAYE_PATH));

        DecomposedSwagger decomposed = new DecomposeSwaggerImpl(getSwaggerNode).execute();

        log.info("decoposed swagger: {}", decomposed);

        PersistDecomposedSwagger persistDecomposedSwagger = new PersistDecomposedSwaggerImpl(
                "src/main/resources/gene-res");

        persistDecomposedSwagger.persist(decomposed);
    }
}