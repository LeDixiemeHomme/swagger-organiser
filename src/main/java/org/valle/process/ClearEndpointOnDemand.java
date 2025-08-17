package org.valle.process;

import org.valle.process.models.EndPoint;

import java.util.Map;
import java.util.Set;

public interface ClearEndpointOnDemand {
    Map<String, Object> execute(Set<EndPoint> toBeCleared);
}
