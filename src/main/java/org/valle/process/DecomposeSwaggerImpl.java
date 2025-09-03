package org.valle.process;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;

@Slf4j
@AllArgsConstructor
public class DecomposeSwaggerImpl implements DecomposeSwagger {

    private final GetSwaggerNode getSwaggerNode;

    @Override
    public DecomposedSwagger execute() {

        SwaggerNode swaggerNode = this.getSwaggerNode.provide();

        // ici on récupère les paths et components
        SwaggerNode paths = swaggerNode.addPathFileReferences().decomposePaths();
        SwaggerNode components = swaggerNode.addComponentFileReferences().decomposeComponent();

        SwaggerNode main = swaggerNode
                .removeComponents()
                .changePathReferences();

        return DecomposedSwagger.builder()
                .main(main)
                .paths(paths)
                .components(components)
                .build();
    }
}
