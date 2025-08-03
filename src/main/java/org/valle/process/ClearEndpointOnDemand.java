package org.valle.process;

import org.valle.process.models.EndPoint;

import java.util.List;

public interface ClearEndpointOnDemand {
    void execute(List<EndPoint> toBeCleared);
}
