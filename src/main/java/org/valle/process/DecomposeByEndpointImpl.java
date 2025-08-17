package org.valle.process;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.provide.GetAllEndpoints;
import org.valle.provide.GetSwaggerRawValue;
import org.valle.provide.GetSwaggerValue;

import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
public class DecomposeByEndpointImpl implements DecomposeByEndpoint {

    private final GetAllEndpoints getAllEndpoints;

    private final GetSwaggerRawValue getSwaggerRawValue;

    private final GetSwaggerValue getSwaggerValue;

    @Override
    public List<Map<String, Object>> execute() {
        return List.of();
    }
}
