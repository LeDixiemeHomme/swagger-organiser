package org.valle.process;

import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;

import java.util.Set;

/**
 * Conserve uniquement les endpoints fournis, supprime tous les autres ainsi que les composants
 * qui leur sont exclusivement associés.
 */
public interface KeepEndpointOnDemand {
    SwaggerNode execute(Set<EndPoint> toKeep);
}

