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
        // 1 - Récupérer le contenu du swagger
        SwaggerNode swaggerNode = this.getSwaggerNode.provide();

        // 2 - Modifie les ref des paths et components pour ajouter les ref des fichiers components
        SwaggerNode swaggerWithRefs = swaggerNode
                .addPathFileReferences()
                .addComponentFileReferences();

        // 3 - Récupère le contenu des paths et components
        SwaggerNode paths = swaggerWithRefs.decomposePaths();
        SwaggerNode components = swaggerWithRefs.decomposeComponent();

        // 4 - Supprime les components du swagger principal
        SwaggerNode decomposedMain = swaggerWithRefs
                .removeComponents()
                // 5 - Modifie les refs des paths pour ajouter les refs des fichiers paths
                .changePathReferences();

        return DecomposedSwagger.builder()
                .main(decomposedMain)
                .paths(paths)
                .components(components)
                .build();
    }
}
