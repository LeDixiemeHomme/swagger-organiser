package org.valle.process;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.SwaggerNode;
import org.valle.process.models.SwaggerRawValue;
import org.valle.provide.GetSwaggerNode;
import org.valle.provide.GetSwaggerRawValue;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@AllArgsConstructor
public class DecomposeSwaggerImpl implements DecomposeSwagger {

    private final GetSwaggerRawValue getSwaggerRawValue;

    private final GetSwaggerNode getSwaggerNode;

    @Override
    public Map<String, Object> execute() {

        SwaggerNode swaggerNode = this.getSwaggerNode.provide();

        SwaggerNode.addPathReference(swaggerNode.node().get("paths"));
        SwaggerNode.addFileReference(swaggerNode.node().get("components"));

        SwaggerRawValue swaggerRawValue = this.getSwaggerRawValue.provide();

        // remplacer les ref par des ref vers les fichiers externes

        SwaggerRawValue rawValueWithRef = swaggerRawValue
                .removeComponents()
//                .removePaths()
                .changePathReferences();
//                .changeComponentsReferences();

        Map<String, Object> decomposed = new HashMap<>();
        decomposed.put("paths", swaggerNode.decomposePaths());
        decomposed.put("components", swaggerNode.decomposeComponent());
        decomposed.put("main", rawValueWithRef.rawValue());

        return decomposed;
    }
}
