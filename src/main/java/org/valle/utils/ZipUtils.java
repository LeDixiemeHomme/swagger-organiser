package org.valle.utils;

import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.Extension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utilitaire de construction d'archives ZIP à partir d'un {@link DecomposedSwagger}.
 */
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

            if (decomposed.paths() != null) {
                decomposed.paths().node().fields().forEachRemaining(e ->
                        addEntrySilent(zos, "paths/" + e.getKey() + "." + ext,
                                JacksonUtils.writeValueAsBytes(e.getValue(), extension)));
            }
            if (decomposed.components() != null) {
                decomposed.components().node().fields().forEachRemaining(e ->
                        addEntrySilent(zos, "components/" + e.getKey() + "." + ext,
                                JacksonUtils.writeValueAsBytes(e.getValue(), extension)));
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

