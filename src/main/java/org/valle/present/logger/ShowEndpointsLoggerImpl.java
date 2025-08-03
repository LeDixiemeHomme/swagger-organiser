package org.valle.present.logger;

import lombok.extern.slf4j.Slf4j;
import org.valle.present.ShowEndpoints;
import org.valle.process.models.EndPoint;

import java.util.Set;

@Slf4j
public class ShowEndpointsLoggerImpl implements ShowEndpoints {
    @Override
    public void display(Set<EndPoint> endPoints) {
        endPoints.forEach(endPoint -> {
            log.info(endPoint.toString());
        });
    }
}
