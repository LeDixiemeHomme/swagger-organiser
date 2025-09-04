package org.valle.provide.fromnode;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;

@Slf4j
@AllArgsConstructor
public class GetSwaggerNodeFromNodeImpl implements GetSwaggerNode {

    private final SwaggerNode swaggerNode;

    @Override
    public SwaggerNode provide() {
        return swaggerNode;
    }
}
