package org.valle.provide.fromstring.jackson;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;

import static org.valle.utils.JacksonUtils.getSwaggerNode;

@Slf4j
@AllArgsConstructor
public class GetSwaggerNodeJacksonFromStringImpl implements GetSwaggerNode {

    private final String swaggerString;

    private final Extension extension;

    @Override
    public SwaggerNode provide() {
        return getSwaggerNode(swaggerString, extension);
    }
}
