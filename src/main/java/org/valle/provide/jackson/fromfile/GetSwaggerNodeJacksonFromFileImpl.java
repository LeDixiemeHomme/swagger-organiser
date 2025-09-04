package org.valle.provide.jackson.fromfile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;

import java.io.File;

import static org.valle.utils.JacksonUtils.getSwaggerNode;

@Slf4j
@AllArgsConstructor
public class GetSwaggerNodeJacksonFromFileImpl implements GetSwaggerNode {

    private final File swaggerFile;

    @Override
    public SwaggerNode provide() {
        return getSwaggerNode(swaggerFile);
    }
}
