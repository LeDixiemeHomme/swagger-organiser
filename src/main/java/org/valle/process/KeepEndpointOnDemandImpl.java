package org.valle.process;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@AllArgsConstructor
public class KeepEndpointOnDemandImpl implements KeepEndpointOnDemand {

    private final GetSwaggerNode getSwaggerNode;

    @Override
    public SwaggerNode execute(Set<EndPoint> toKeep) {
        // 1 - Récupérer le contenu du swagger
        SwaggerNode swaggerNode = this.getSwaggerNode.provide();

        // 2 - Calculer les endpoints à supprimer = tous les endpoints - ceux à conserver
        Set<EndPoint> toRemove = new HashSet<>(swaggerNode.getAllEndpoints());
        toRemove.removeAll(toKeep);

        log.debug("KeepEndpoints — {} endpoint(s) conservé(s), {} endpoint(s) à supprimer",
                toKeep.size(), toRemove.size());

        // 3 - Récupérer les schémas associés uniquement aux endpoints supprimés
        Set<String> schemasToRemove = swaggerNode.getSchemaNamesToBeRemoved(toRemove);

        // 4 - Supprimer les endpoints et les schémas non utilisés
        SwaggerNode result = swaggerNode.removeElementsByName(toRemove, schemasToRemove);

        log.debug("KeepEndpoints — swagger résultant : {}", result);

        return result;
    }
}

