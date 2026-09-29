package org.valle.provide.fromstring.jackson;

import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;

import static org.valle.utils.JacksonUtils.getSwaggerNode;

@Slf4j
public class GetSwaggerNodeJacksonFromStringImpl implements GetSwaggerNode {

    private final String swaggerString;

    private final Extension extension;
    private final boolean preserveComments;

    public GetSwaggerNodeJacksonFromStringImpl(String swaggerString, Extension extension) {
        this(swaggerString, extension, true);
    }

    public GetSwaggerNodeJacksonFromStringImpl(
            String swaggerString, Extension extension, boolean preserveComments) {
        this.swaggerString = swaggerString;
        this.extension = extension;
        this.preserveComments = preserveComments;
    }

    @Override
    public SwaggerNode provide() {
        return getSwaggerNode(swaggerString, extension, preserveComments);
    }
}
