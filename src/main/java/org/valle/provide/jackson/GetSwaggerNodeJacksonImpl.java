package org.valle.provide.jackson;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;

@Slf4j
@AllArgsConstructor
public class GetSwaggerNodeJacksonImpl implements GetSwaggerNode {

    private final JacksonUtils jacksonUtils;

    @Override
    public SwaggerNode provide() {
        return new SwaggerNode(jacksonUtils.readValue());
    }
}
