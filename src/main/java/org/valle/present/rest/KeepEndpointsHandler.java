package org.valle.present.rest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.KeepEndpointOnDemand;
import org.valle.process.KeepEndpointOnDemandImpl;
import org.valle.process.models.EndPoint;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.fromstring.jackson.GetSwaggerNodeJacksonFromStringImpl;
import org.valle.utils.ZipUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handler REST — {@code POST /keep-endpoints}
 *
 * <p>Conserve un ou plusieurs endpoints d'un fichier Swagger (JSON ou YAML) et supprime tous les
 * autres ainsi que les schémas de composants qui leur sont exclusivement associés. Retourne le
 * swagger filtré sous forme d'archive ZIP.
 *
 * <p>Un schéma partagé par un endpoint conservé et un endpoint supprimé est <em>gardé</em>.
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
 *   <tr>
 *     <td>{@code endpoints}</td><td>Oui</td>
 *     <td>Liste des endpoints à <strong>conserver</strong>, séparés par des virgules,
 *         format {@code method:path}.<br>
 *         Ex : {@code get:/cadh/v1/operations,post:/cadh/v1/operations/{id}/documents}</td>
 *   </tr>
 * </table>
 *
 * <h3>Corps de la requête</h3>
 * <ul>
 *   <li><b>multipart/form-data</b> — champ nommé {@code file} (recommandé, compatible Bruno/curl {@code -F})</li>
 *   <li><b>Corps brut</b> — {@code application/octet-stream} (compatible curl {@code --data-binary})</li>
 * </ul>
 *
 * <h3>Réponse</h3>
 * <ul>
 *   <li>{@code 200 OK} — archive ZIP ({@code Content-Type: application/zip}) contenant
 *       un unique fichier Swagger filtré :
 *     <pre>
 * swagger-kept.zip
 * └── swagger-kept.yml   (ou .json selon l'extension fournie)
 *     </pre>
 *   </li>
 *   <li>{@code 400 Bad Request} — paramètre manquant, endpoint introuvable ou format invalide</li>
 *   <li>{@code 405 Method Not Allowed} — méthode HTTP autre que POST</li>
 *   <li>{@code 500 Internal Server Error} — erreur inattendue côté serveur</li>
 * </ul>
 *
 * <h3>Exemple curl</h3>
 * <pre>
 * curl -X POST \
 *   "http://localhost:8080/keep-endpoints?extension=yml&endpoints=get:/cadh/v1/operations" \
 *   -F "file=@swagger.yml" --output swagger-kept.zip
 * </pre>
 *
 * @see ClearEndpointsHandler pour supprimer des endpoints spécifiques
 */
@Slf4j
public class KeepEndpointsHandler implements HttpHandler {

    /** Crée le service de filtrage d'endpoints à partir du contenu et de l'extension. */
    @FunctionalInterface
    interface KeepFactory {
        KeepEndpointOnDemand create(String content, Extension extension);
    }

    /** Construit l'archive ZIP à partir du nœud Swagger filtré et du nom de fichier. */
    @FunctionalInterface
    interface ZipBuildFactory {
        byte[] build(SwaggerNode node, String filename) throws IOException;
    }

    // Package-private pour injection dans les tests
    KeepFactory keepFactory = (content, ext) ->
            new KeepEndpointOnDemandImpl(new GetSwaggerNodeJacksonFromStringImpl(content, ext));

    ZipBuildFactory zipBuildFactory = ZipUtils::buildFromNode;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            RestUtils.sendError(exchange, 405, "Méthode non supportée — utilisez POST.");
            return;
        }
        try {
            Map<String, String> params = RestUtils.parseQuery(exchange.getRequestURI().getQuery());

            String extensionParam = params.get("extension");
            String endpointsParam = params.get("endpoints");

            if (extensionParam == null || extensionParam.isBlank()) {
                RestUtils.sendError(exchange, 400, "Paramètre 'extension' manquant (json, yml, yaml).");
                return;
            }
            if (endpointsParam == null || endpointsParam.isBlank()) {
                RestUtils.sendError(exchange, 400,
                        "Paramètre 'endpoints' manquant (ex: get:/path,post:/path2).");
                return;
            }

            byte[] fileBytes = RestUtils.readFileBytes(exchange);
            if (fileBytes.length == 0) {
                RestUtils.sendError(exchange, 400,
                        "Le corps de la requête est vide — envoyez le fichier Swagger.");
                return;
            }

            Set<EndPoint> endpointsToKeep = Arrays.stream(endpointsParam.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(EndPoint::fromString)
                    .collect(Collectors.toSet());

            Extension extension = Extension.valueOf(extensionParam.toUpperCase());

            log.info("REST KeepEndpoints — extension={}, {} endpoint(s) à conserver, {} octets",
                    extension, endpointsToKeep.size(), fileBytes.length);

            // 1 — Conserver uniquement les endpoints demandés
            String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
            SwaggerNode keptNode = keepFactory.create(fileContent, extension)
                    .execute(endpointsToKeep);

            // 2 — Zipper le fichier filtré
            String filename = "swagger-kept." + extensionParam.toLowerCase();
            byte[] zipBytes = zipBuildFactory.build(keptNode, filename);

            exchange.getResponseHeaders().set("Content-Disposition",
                    "attachment; filename=\"swagger-kept.zip\"");
            RestUtils.sendBytes(exchange, 200, "application/zip", zipBytes);

            log.info("REST KeepEndpoints — {} endpoint(s) conservé(s), ZIP retourné ({} octets)",
                    endpointsToKeep.size(), zipBytes.length);

        } catch (IllegalArgumentException e) {
            log.warn("REST KeepEndpoints — paramètre invalide : {}", e.getMessage());
            RestUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            log.error("REST KeepEndpoints — erreur inattendue", e);
            RestUtils.sendError(exchange, 500, "Erreur interne : " + e.getMessage());
        }
    }
}

