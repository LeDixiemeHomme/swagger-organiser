package org.valle.provide.fromfile.jackson;

import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;

import java.io.File;

import static org.valle.utils.JacksonUtils.getSwaggerNode;

@Slf4j
public class GetSwaggerNodeJacksonFromFileImpl implements GetSwaggerNode {

    private final File swaggerFile;
    private final boolean preserveComments;

    public GetSwaggerNodeJacksonFromFileImpl(File swaggerFile) {
        this(swaggerFile, true);
    }

    public GetSwaggerNodeJacksonFromFileImpl(File swaggerFile, boolean preserveComments) {
        this.swaggerFile = swaggerFile;
        this.preserveComments = preserveComments;
    }

    @Override
    public SwaggerNode provide() {
        return getSwaggerNode(swaggerFile, preserveComments);
    }
}
