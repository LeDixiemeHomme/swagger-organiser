package org.valle.process;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.EndPoint;
import org.valle.provide.GetAllEndpoints;
import org.valle.provide.GetSwaggerNode;
import org.valle.provide.GetSwaggerRawValue;

import java.util.Map;
import java.util.Set;

@Slf4j
@AllArgsConstructor
public class ClearEndpointOnDemandImpl implements ClearEndpointOnDemand {

    private final GetAllEndpoints getAllEndpoints;

    private final GetSwaggerRawValue getSwaggerRawValue;

    private final GetSwaggerNode getSwaggerNode;

    @Override
    public Map<String, Object> execute(Set<EndPoint> toBeCleared) {

        Set<String> schemasToRemove = this.getSwaggerNode.provide().getSchemaNamesToBeRemoved(
                this.getAllEndpoints.provide(),
                toBeCleared
        );

        Map<String, Object> clearedSwagger = this.getSwaggerRawValue.provide().removeElementsByName(toBeCleared, schemasToRemove);

        log.info("Swagger cleared: {}", clearedSwagger);

        return clearedSwagger;
    }
}
