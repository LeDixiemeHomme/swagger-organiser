package org.valle.process;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;

import java.util.Set;

@Slf4j
@AllArgsConstructor
public class ClearEndpointOnDemandImpl implements ClearEndpointOnDemand {

    private final GetSwaggerNode getSwaggerNode;

    @Override
    public SwaggerNode execute(Set<EndPoint> toBeCleared) {
        // 1 - Récupérer le contenu du swagger
        SwaggerNode swaggerNode = this.getSwaggerNode.provide();

        // 2 - Récupérer les schemas associés aux endpoints à supprimer
        Set<String> schemasToRemove = swaggerNode.getSchemaNamesToBeRemoved(toBeCleared);

        // 3 - Supprimer les endpoints et les schemas associés
        SwaggerNode clearedSwagger = swaggerNode.removeElementsByName(toBeCleared, schemasToRemove);

        log.debug("Swagger cleared: {}", clearedSwagger);

        return clearedSwagger;
    }
}
