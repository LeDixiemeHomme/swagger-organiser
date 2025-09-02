package org.valle.process;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@AllArgsConstructor
public class DecomposeSwaggerImpl implements DecomposeSwagger {

    private final GetSwaggerNode getSwaggerNode;

    @Override
    public Map<String, SwaggerNode> execute() {

        SwaggerNode swaggerNode = this.getSwaggerNode.provide();

        Map<String, SwaggerNode> decomposed = new HashMap<>();

        decomposed.put("paths", swaggerNode.addPathReference().decomposePaths());
        decomposed.put("components", swaggerNode.addFileReference().decomposeComponent());

        decomposed.put("main", swaggerNode
                .removeComponents()
                .changePathReferences());

        return decomposed;
    }
}
