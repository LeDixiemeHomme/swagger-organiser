package org.valle.process;

import org.valle.process.models.EndPoint;

import java.util.Set;

public interface ClearEndpointOnDemand {
    void execute(Set<EndPoint> toBeCleared);
}
