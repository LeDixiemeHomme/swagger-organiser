package org.valle.persist.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.persist.PersistDecomposedSwagger;
import org.valle.process.models.DecomposedSwagger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.valle.utils.SafePathResolver.resolveWithin;

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
        try {
            Files.createDirectories(pathsDirectory.toPath());
            Files.createDirectories(componentsDirectory.toPath());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Impossible de créer le répertoire de décomposition.", exception);
        }

        if (toPersist.paths() != null) {
            toPersist.paths().node().fields().forEachRemaining(entry -> {
                Path path = resolveWithin(pathsDirectory.toPath(),
                        "%s.%s".formatted(entry.getKey(), strExtension), "Nom de fichier path");
                File file = path.toFile();
                new PersistResultNodeImpl(file).persist((ObjectNode) entry.getValue());
            });
        }

        if (toPersist.components() != null) {
            toPersist.components().node().fields().forEachRemaining(entry -> {
                Path path = resolveWithin(componentsDirectory.toPath(),
                        "%s.%s".formatted(entry.getKey(), strExtension), "Nom de fichier composant");
                File file = path.toFile();
                new PersistResultNodeImpl(file).persist((ObjectNode) entry.getValue());
            });
        }

        if (!toPersist.componentCategories().isEmpty()) {
            ObjectNode metadata = new ObjectMapper().createObjectNode();
            toPersist.componentCategories()
                    .forEach(metadata::put);
            new PersistResultNodeImpl(new File(baseDirectory,
                    DecomposedSwagger.COMPONENT_CATEGORIES_FILE)).persist(metadata);
        }

        File file = new File(baseDirectory, "main.%s".formatted(strExtension));
        new PersistResultNodeImpl(file).persist((ObjectNode) toPersist.main().node());
    }
}
