package org.valle.process;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetAllEndpoints;
import org.valle.provide.GetSwaggerNode;

import java.util.Set;

@Slf4j
@AllArgsConstructor
public class ClearEndpointOnDemandImpl implements ClearEndpointOnDemand {

    private final GetAllEndpoints getAllEndpoints;

    private final GetSwaggerNode getSwaggerNode;

    @Override
    public SwaggerNode execute(Set<EndPoint> toBeCleared) {

        SwaggerNode swaggerNode = this.getSwaggerNode.provide();

        Set<String> schemasToRemove = swaggerNode.getSchemaNamesToBeRemoved(
                this.getAllEndpoints.provide(),
                toBeCleared
        );
        SwaggerNode clearedSwagger = swaggerNode.removeElementsByName(toBeCleared, schemasToRemove);

        log.debug("Swagger cleared: {}", clearedSwagger);

        return clearedSwagger;
    }
}
