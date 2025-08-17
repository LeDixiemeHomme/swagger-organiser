package org.valle.provide.jackson;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerValue;

@Slf4j
@AllArgsConstructor
public class GetSwaggerValueJacksonImpl implements GetSwaggerValue {

    private final JacksonUtils jacksonUtils;

    @Override
    public SwaggerNode provide() {
        return new SwaggerNode(jacksonUtils.readValue());
    }
}
