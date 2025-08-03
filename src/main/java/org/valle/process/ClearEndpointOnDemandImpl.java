package org.valle.process;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.EndPoint;
import org.valle.provide.GetAllEndpoints;
import org.valle.provide.GetAllSchemas;
import org.valle.provide.GetOpenApiObjects;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class ClearEndpointOnDemandImpl implements ClearEndpointOnDemand {

    private final GetAllEndpoints getAllEndpoints;
    private final GetAllSchemas getAllSchemas;
    private final GetOpenApiObjects getOpenApiObjects;

    @Override
    public void execute(List<EndPoint> toBeCleared) {

    }
}
