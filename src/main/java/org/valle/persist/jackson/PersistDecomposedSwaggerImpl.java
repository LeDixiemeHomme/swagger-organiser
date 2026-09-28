package org.valle.persist.jackson;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.persist.PersistDecomposedSwagger;
import org.valle.process.models.DecomposedSwagger;

import java.io.File;

@Slf4j
@AllArgsConstructor
public class PersistDecomposedSwaggerImpl implements PersistDecomposedSwagger {

    private final String basePath;

    @Override
    public void persist(DecomposedSwagger toPersist) {
        String strExtension = toPersist.getExtension().toString().toLowerCase();

        File baseDirectory = new File(basePath);
        File pathsDirectory = new File(baseDirectory, "paths");
        File componentsDirectory = new File(baseDirectory, "components");
        pathsDirectory.mkdirs();
        componentsDirectory.mkdirs();

        if (toPersist.paths() != null) {
            toPersist.paths().node().fields().forEachRemaining(entry -> {
                File file = new File(pathsDirectory, "%s.%s".formatted(entry.getKey(), strExtension));
                new PersistResultNodeImpl(file).persist((ObjectNode) entry.getValue());
            });
        }

        if (toPersist.components() != null) {
            toPersist.components().node().fields().forEachRemaining(entry -> {
                File file = new File(componentsDirectory, "%s.%s".formatted(entry.getKey(), strExtension));
                new PersistResultNodeImpl(file).persist((ObjectNode) entry.getValue());
            });
        }

        File file = new File(baseDirectory, "main.%s".formatted(strExtension));
        new PersistResultNodeImpl(file).persist((ObjectNode) toPersist.main().node());
    }
}
