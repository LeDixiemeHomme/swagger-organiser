package org.valle.process;

import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;

import java.util.Set;

public interface ClearEndpointOnDemand {
    SwaggerNode execute(Set<EndPoint> toBeCleared);
}
