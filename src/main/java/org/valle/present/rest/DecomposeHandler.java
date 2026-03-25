package org.valle.present.rest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.DecomposeSwagger;
import org.valle.process.DecomposeSwaggerImpl;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.Extension;
import org.valle.provide.fromstring.jackson.GetSwaggerNodeJacksonFromStringImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Handler REST — {@code POST /swagger/decompose}
 *
 * <p>Décompose un fichier Swagger (JSON ou YAML) en plusieurs fichiers organisés par
 * responsabilité ({@code main}, {@code paths}, {@code components}), et retourne le tout
 * dans une archive ZIP.
 *
 * <p>La décomposition fonctionne en trois étapes :
 * <ol>
 *   <li>Extraction du fichier principal ({@code main.{ext}}) avec des {@code $ref} vers les sous-fichiers.</li>
 *   <li>Extraction de chaque path dans un fichier dédié ({@code paths/{nom}.{ext}}).</li>
 *   <li>Extraction de chaque schéma de composant dans un fichier dédié ({@code components/{nom}.{ext}}).</li>
 * </ol>
 *
 * <h3>Méthode HTTP</h3>
 * {@code POST} — toute autre méthode retourne {@code 405 Method Not Allowed}.
 *
 * <h3>Paramètres (query string)</h3>
 * <table border="1">
 *   <tr><th>Paramètre</th><th>Obligatoire</th><th>Description</th></tr>
 *   <tr>
 *     <td>{@code extension}</td><td>Oui</td>
 *     <td>Format du fichier : {@code json}, {@code yml} ou {@code yaml}</td>
 *   </tr>
 * </table>
 *
 * <h3>Corps de la requête</h3>
 * <p>Le fichier Swagger peut être envoyé de deux façons :
 * <ul>
 *   <li><b>multipart/form-data</b> — champ nommé {@code file} (recommandé, compatible Bruno/curl {@code -F})</li>
 *   <li><b>Corps brut</b> — {@code application/octet-stream} ou sans Content-Type (compatible curl {@code --data-binary})</li>
 * </ul>
 *
 * <h3>Réponse</h3>
 * <ul>
 *   <li>{@code 200 OK} — archive ZIP ({@code Content-Type: application/zip}) structurée ainsi :
 *     <pre>
 * swagger-decomposed.zip
 * ├── main.yml                         ← fichier principal avec $ref vers paths/ et components/
 * ├── paths/
 * │   ├── cadh-v1-operations.yml       ← un fichier par path (slashes → tirets, accolades retirées)
 * │   └── ...
 * └── components/
 *     ├── OperationDTO.yml             ← un fichier par schéma de composant
 *     └── ...
 *     </pre>
 *   </li>
 *   <li>{@code 400 Bad Request} — paramètre manquant ou format de fichier invalide</li>
 *   <li>{@code 405 Method Not Allowed} — méthode HTTP autre que POST</li>
 *   <li>{@code 500 Internal Server Error} — erreur inattendue côté serveur</li>
 * </ul>
 *
 * <h3>Exemple</h3>
 * <pre>
 * curl -X POST \
 *   "http://localhost:8080/swagger/decompose?extension=yml" \
 *   -F "file=@swagger.yml" --output swagger-decomposed.zip
 * </pre>
 *
 * @see ClearEndpointsHandler pour supprimer des endpoints avant de décomposer
 */
@Slf4j
public class DecomposeHandler implements HttpHandler {

    /** Crée le service de décomposition à partir du contenu et de l'extension du fichier. */
    @FunctionalInterface
    interface DecomposeFactory {
        DecomposeSwagger create(String content, Extension extension);
    }

    /** Construit l'archive ZIP à partir d'un swagger décomposé. */
    @FunctionalInterface
    interface ZipBuildFactory {
        byte[] build(DecomposedSwagger decomposed) throws IOException;
    }

    // Package-private pour injection dans les tests
    DecomposeFactory decomposeFactory = (content, ext) ->
            new DecomposeSwaggerImpl(new GetSwaggerNodeJacksonFromStringImpl(content, ext));

    ZipBuildFactory zipBuildFactory = RestUtils::buildZip;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            RestUtils.sendError(exchange, 405, "Méthode non supportée — utilisez POST.");
            return;
        }
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
                        "Le corps de la requête est vide — envoyez le fichier Swagger.");
                return;
            }

            Extension extension = Extension.valueOf(extensionParam.toUpperCase());

            log.info("REST Decompose — extension={}, {} octets reçus", extension, fileBytes.length);

            String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
            DecomposedSwagger decomposed = decomposeFactory.create(fileContent, extension).execute();

            byte[] zipBytes = zipBuildFactory.build(decomposed);

            exchange.getResponseHeaders().set("Content-Disposition",
                    "attachment; filename=\"swagger-decomposed.zip\"");
            RestUtils.sendBytes(exchange, 200, "application/zip", zipBytes);

            log.info("REST Decompose — archive ZIP retournée ({} octets)", zipBytes.length);

        } catch (IllegalArgumentException e) {
            log.warn("REST Decompose — paramètre invalide : {}", e.getMessage());
            RestUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            log.error("REST Decompose — erreur inattendue", e);
            RestUtils.sendError(exchange, 500, "Erreur interne : " + e.getMessage());
        }
    }
}

