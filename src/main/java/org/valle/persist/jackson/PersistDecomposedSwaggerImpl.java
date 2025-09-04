package org.valle.persist.jackson;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.valle.persist.PersistDecomposedSwagger;
import org.valle.process.models.DecomposedSwagger;
import org.valle.utils.JacksonUtils;

import java.io.File;

@Slf4j
@AllArgsConstructor
public class PersistDecomposedSwaggerImpl implements PersistDecomposedSwagger {

    private final String basePath;

    @Override
    public void persist(DecomposedSwagger toPersist) {
        String strExtension = toPersist.getExtension().toString().toLowerCase();

        toPersist.paths().node().fields().forEachRemaining(entry -> {
            File pathsDir = new File(basePath + "/paths");
            if (pathsDir.exists()) {
                pathsDir.delete();
            }
            pathsDir.mkdirs();
            JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File(basePath + "/paths/%s.%s".formatted(entry.getKey(), strExtension)));
            new PersistResultNodeImpl(jacksonUtilsTmp).persist((ObjectNode) entry.getValue());
        });

        toPersist.components().node().fields().forEachRemaining(entry -> {
            File pathsDir = new File(basePath + "/components");
            if (pathsDir.exists()) {
                pathsDir.delete();
            }
            pathsDir.mkdirs();
            JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File(basePath + "/components/%s.%s".formatted(entry.getKey(), strExtension)));
            new PersistResultNodeImpl(jacksonUtilsTmp).persist((ObjectNode) entry.getValue());
        });

        JacksonUtils jacksonUtilsTmp = new JacksonUtils(new File(basePath + "/main.%s".formatted(strExtension)));
        new PersistResultNodeImpl(jacksonUtilsTmp).persist((ObjectNode) toPersist.main().node());
    }
}
