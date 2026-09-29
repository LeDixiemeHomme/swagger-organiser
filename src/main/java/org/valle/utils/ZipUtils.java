package org.valle.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.Extension;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utilitaire de construction d'archives ZIP à partir d'un {@link DecomposedSwagger}.
 */
@Slf4j
public class ZipUtils {

    private ZipUtils() {}

    /**
     * Construit une archive ZIP contenant un unique fichier Swagger nettoyé.
     *
     * @param node     le swagger nettoyé
     * @param filename nom du fichier dans l'archive (ex : {@code swagger-cleared.yml})
     * @return les octets de l'archive ZIP
     * @throws IOException en cas d'erreur de sérialisation
     */
    public static byte[] buildFromNode(org.valle.process.models.SwaggerNode node, String filename)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            addEntry(zos, filename, JacksonUtils.writeValueAsBytes(node));
        }
        return baos.toByteArray();
    }

    /**
     * Construit une archive ZIP contenant les fichiers d'un swagger décomposé.
     *
     * <p>Structure de l'archive :
     * <pre>
     * main.{ext}
     * paths/{nom}.{ext}
     * components/{nom}.{ext}
     * component-categories.json (si des composants sont présents)
     * </pre>
     *
     * @param decomposed le swagger décomposé
     * @return les octets de l'archive ZIP
     * @throws IOException en cas d'erreur de sérialisation
     */
    public static byte[] build(DecomposedSwagger decomposed) throws IOException {
        String ext        = decomposed.getExtension().toString().toLowerCase();
        Extension extension = decomposed.getExtension();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            addEntry(zos, "main." + ext, JacksonUtils.writeValueAsBytes(decomposed.main()));
            log.debug("ZIP entry generated: name=main.{}, comments={}, anchors={}",
                    ext, decomposed.main().commentsByNode().size(),
                    decomposed.main().commentsByAnchor().size());

            if (decomposed.paths() != null) {
                decomposed.paths().node().fields().forEachRemaining(e -> {
                        addEntrySilent(zos, "paths/" + e.getKey() + "." + ext,
                                        JacksonUtils.writeValueAsBytes(
                                                decomposed.paths().toBuilder()
                                                        .node(e.getValue())
                                                        .comments(decomposed.paths().commentsFor("paths/" + e.getKey()))
                                                        .build()));
                        log.debug("ZIP path entry generated: name=paths/{}.{} comments={}",
                                e.getKey(), ext,
                                decomposed.paths().commentsFor("paths/" + e.getKey()).lines()
                                        .filter(line -> line.stripLeading().startsWith("#")).count());
                });
            }
            if (decomposed.components() != null) {
                decomposed.components().node().fields().forEachRemaining(e -> {
                        addEntrySilent(zos, "components/" + e.getKey() + "." + ext,
                                JacksonUtils.writeValueAsBytes(
                                        decomposed.components().toBuilder()
                                                .node(e.getValue())
                                                .comments(decomposed.components().commentsFor("components/" + e.getKey()))
                                                .build()));
                        log.debug("ZIP component entry generated: name=components/{}.{} comments={}",
                                e.getKey(), ext,
                                decomposed.components().commentsFor("components/" + e.getKey()).lines()
                                        .filter(line -> line.stripLeading().startsWith("#")).count());
                });
            }
            if (!decomposed.componentCategories().isEmpty()) {
                ObjectNode metadata = new ObjectMapper().createObjectNode();
                decomposed.componentCategories().forEach(metadata::put);
                addEntry(zos, DecomposedSwagger.COMPONENT_CATEGORIES_FILE,
                        JacksonUtils.writeValueAsBytes(metadata, Extension.JSON));
            }
        }
        return baos.toByteArray();
    }

    private static void addEntry(ZipOutputStream zos, String name, byte[] bytes) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(bytes);
        zos.closeEntry();
    }

    private static void addEntrySilent(ZipOutputStream zos, String name, byte[] bytes) {
        try {
            addEntry(zos, name, bytes);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'ajout de l'entrée ZIP : " + name, e);
        }
    }
}
