package org.valle.present.rest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.MergeSwagger;
import org.valle.process.MergeSwaggerImpl;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.fromfile.jackson.GetSwaggerNodeJacksonFromFileImpl;
import org.valle.utils.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Handler REST — {@code POST /merge}
 *
 * <p>Fusionne un swagger décomposé (archive ZIP multi-fichiers avec {@code $ref}) en un seul
 * fichier unifié. C'est l'opération inverse de {@code POST /decompose}.
 *
 * <p>Le ZIP reçu doit respecter la structure produite par {@link DecomposeHandler} :
 * <pre>
 * swagger-decomposed.zip
 * ├── main.yml              ← fichier principal avec $ref vers paths/ et components/
 * ├── paths/
 * │   ├── mon-path.yml
 * │   └── ...
 * └── components/
 *     ├── MonSchema.yml
 *     └── ...
 * </pre>
 *
 * <h3>Méthode HTTP</h3>
 * {@code POST} — toute autre méthode retourne {@code 405 Method Not Allowed}.
 *
 * <h3>Paramètres (query string)</h3>
 * <table border="1">
 *   <tr><th>Paramètre</th><th>Obligatoire</th><th>Description</th></tr>
 *   <tr>
 *     <td>{@code extension}</td><td>Oui</td>
 *     <td>Format du fichier de sortie : {@code json}, {@code yml} ou {@code yaml}</td>
 *   </tr>
 * </table>
 *
 * <h3>Corps de la requête</h3>
 * <ul>
 *   <li><b>multipart/form-data</b> — champ nommé {@code file} (recommandé, compatible Bruno/curl {@code -F})</li>
 *   <li><b>Corps brut</b> — {@code application/octet-stream}</li>
 * </ul>
 *
 * <h3>Réponse</h3>
 * <ul>
 *   <li>{@code 200 OK} — archive ZIP ({@code Content-Type: application/zip}) contenant
 *       un unique fichier swagger fusionné :
 *     <pre>
 * swagger-merged.zip
 * └── swagger-merged.yml   (ou .json selon l'extension fournie)
 *     </pre>
 *   </li>
 *   <li>{@code 400 Bad Request} — paramètre manquant, format invalide ou fichier {@code main.*} introuvable dans le ZIP</li>
 *   <li>{@code 405 Method Not Allowed} — méthode HTTP autre que POST</li>
 *   <li>{@code 500 Internal Server Error} — erreur inattendue côté serveur</li>
 * </ul>
 *
 * <h3>Exemple</h3>
 * <pre>
 * curl -X POST \
 *   "http://localhost:8080/merge?extension=yml" \
 *   -F "file=@swagger-decomposed.zip" --output swagger-merged.zip
 * </pre>
 *
 * @see DecomposeHandler pour décomposer un swagger en plusieurs fichiers
 */
@Slf4j
public class MergeHandler implements HttpHandler {

    /** Crée le service de fusion à partir du fichier principal et du répertoire de base. */
    @FunctionalInterface
    interface MergeFactory {
        MergeSwagger create(File mainFile, File baseDir);
    }

    /** Construit l'archive ZIP à partir du nœud Swagger fusionné et du nom de fichier. */
    @FunctionalInterface
    interface ZipBuildFactory {
        byte[] build(SwaggerNode node, String filename) throws IOException;
    }

    // Package-private pour injection dans les tests
    MergeFactory mergeFactory = (mainFile, baseDir) ->
            new MergeSwaggerImpl(new GetSwaggerNodeJacksonFromFileImpl(mainFile), baseDir);

    ZipBuildFactory zipBuildFactory = ZipUtils::buildFromNode;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            RestUtils.sendError(exchange, 405, "Méthode non supportée — utilisez POST.");
            return;
        }

        Path tempDir = null;
        try {
            Map<String, String> params = RestUtils.parseQuery(exchange.getRequestURI().getQuery());

            String extensionParam = params.get("extension");
            if (extensionParam == null || extensionParam.isBlank()) {
                RestUtils.sendError(exchange, 400, "Paramètre 'extension' manquant (json, yml, yaml).");
                return;
            }

            byte[] fileBytes = RestUtils.readFileBytes(exchange);
            if (fileBytes.length == 0) {
                RestUtils.sendError(exchange, 400,
                        "Le corps de la requête est vide — envoyez l'archive ZIP du swagger décomposé.");
                return;
            }

            Extension extension = Extension.valueOf(extensionParam.toUpperCase());

            log.info("REST Merge — extension={}, {} octets reçus", extension, fileBytes.length);

            // 1 — Extraire le ZIP dans un répertoire temporaire
            tempDir = Files.createTempDirectory("swagger-merge-");
            extractZip(fileBytes, tempDir);

            // 2 — Localiser le fichier principal (main.yml / main.yaml / main.json)
            File mainFile = findMainFile(tempDir);
            if (mainFile == null) {
                RestUtils.sendError(exchange, 400,
                        "Fichier 'main.yml' (ou .yaml/.json) introuvable dans le ZIP fourni.");
                return;
            }

            log.debug("REST Merge — fichier principal trouvé : {}", mainFile.getAbsolutePath());

            // 3 — Fusionner
            SwaggerNode mergedNode = mergeFactory.create(mainFile, tempDir.toFile()).execute();

            // 4 — Zipper le résultat
            String filename = "swagger-merged." + extensionParam.toLowerCase();
            byte[] zipBytes = zipBuildFactory.build(mergedNode, filename);

            exchange.getResponseHeaders().set("Content-Disposition",
                    "attachment; filename=\"swagger-merged.zip\"");
            RestUtils.sendBytes(exchange, 200, "application/zip", zipBytes);

            log.info("REST Merge — fusion terminée, ZIP retourné ({} octets)", zipBytes.length);

        } catch (IllegalArgumentException e) {
            log.warn("REST Merge — paramètre invalide : {}", e.getMessage());
            RestUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            log.error("REST Merge — erreur inattendue", e);
            RestUtils.sendError(exchange, 500, "Erreur interne : " + e.getMessage());
        } finally {
            // Nettoyage du répertoire temporaire
            if (tempDir != null) {
                deleteRecursively(tempDir);
            }
        }
    }

    // ── Utilitaires privés ────────────────────────────────────────────────────

    /**
     * Extrait le contenu d'un ZIP (en mémoire) dans le répertoire cible.
     * Les entrées sont protégées contre les attaques de type "Zip Slip".
     */
    private static void extractZip(byte[] zipBytes, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(
                new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(targetDir)) {
                    throw new IllegalArgumentException(
                            "Entrée ZIP invalide (Zip Slip détecté) : " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.write(entryPath, zis.readAllBytes());
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Recherche le fichier principal ({@code main.yml}, {@code main.yaml} ou {@code main.json})
     * à la racine du répertoire temporaire.
     */
    private static File findMainFile(Path dir) {
        for (String name : new String[]{"main.yml", "main.yaml", "main.json"}) {
            File candidate = dir.resolve(name).toFile();
            if (candidate.exists()) return candidate;
        }
        return null;
    }

    /** Supprime récursivement un répertoire temporaire. */
    private static void deleteRecursively(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(f -> {
                        if (!f.delete()) {
                            log.warn("REST Merge — impossible de supprimer : {}", f);
                        }
                    });
        } catch (IOException e) {
            log.warn("REST Merge — impossible de supprimer le répertoire temporaire : {}", dir, e);
        }
    }
}




