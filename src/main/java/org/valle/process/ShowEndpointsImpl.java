package org.valle.process;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.present.ShowEndpoints;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.jackson.JacksonUtils;

import java.util.Set;

@Slf4j
@AllArgsConstructor
public class ShowEndpointsImpl implements GetAndShowEndpoints {

    private final JacksonUtils jacksonUtils;
    private final ShowEndpoints showEndpoints;

    @Override
    public void execute() {
        Set<EndPoint> endPoints = new SwaggerNode(jacksonUtils.readValue()).getAllEndpoints();
        log.info("Found {} endPoints", endPoints.size());
        showEndpoints.display(endPoints);
    }
}
