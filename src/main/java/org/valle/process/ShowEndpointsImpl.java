package org.valle.process;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.present.ShowEndpoints;
import org.valle.process.models.EndPoint;
import org.valle.provide.GetAllEndpoints;

import java.util.Set;

@Slf4j
@AllArgsConstructor
public class ShowEndpointsImpl implements GetAndShowEndpoints {

    private final GetAllEndpoints getAllEndpoints;
    private final ShowEndpoints showEndpoints;

    @Override
    public void execute() {
        Set<EndPoint> endPoints = getAllEndpoints.provide();
        log.info("Found {} endPoints", endPoints.size());
        showEndpoints.display(endPoints);
    }
}
