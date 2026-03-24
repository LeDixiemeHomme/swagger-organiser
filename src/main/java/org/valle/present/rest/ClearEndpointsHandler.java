package org.valle.present.rest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.ClearEndpointOnDemandImpl;
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
 * Handler REST — {@code POST /swagger/clear-endpoints}
 *
 * <p>Supprime un ou plusieurs endpoints d'un fichier Swagger (JSON ou YAML) ainsi que les schémas
 * de composants qui leur sont exclusivement associés, puis retourne le swagger nettoyé sous forme
 * d'archive ZIP décomposée.
 *
 * <p>La suppression des schémas est intelligente : un schéma partagé par plusieurs endpoints
 * n'est supprimé que si <em>tous</em> les endpoints qui le référencent sont eux-mêmes supprimés.
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
 *     <td>Liste d'endpoints séparés par des virgules, format {@code method:path}.<br>
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
 *       un unique fichier Swagger nettoyé :
 *     <pre>
 * swagger-cleared.zip
 * └── swagger-cleared.yml   (ou .json selon l'extension fournie)
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
 *   "http://localhost:8080/swagger/clear-endpoints?extension=yml&endpoints=get:/cadh/v1/operations" \
 *   -F "file=@swagger.yml" --output swagger-cleared.zip
 * </pre>
 *
 * @see DecomposeHandler pour décomposer un swagger sans suppression d'endpoints
 */
@Slf4j
public class ClearEndpointsHandler implements HttpHandler {

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

            Set<EndPoint> endpointsToRemove = Arrays.stream(endpointsParam.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(EndPoint::fromString)
                    .collect(Collectors.toSet());

            Extension extension = Extension.valueOf(extensionParam.toUpperCase());

            log.info("REST ClearEndpoints — extension={}, {} endpoint(s) à supprimer, {} octets",
                    extension, endpointsToRemove.size(), fileBytes.length);

            // 1 — Supprimer les endpoints
            String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
            SwaggerNode clearedNode = new ClearEndpointOnDemandImpl(
                    new GetSwaggerNodeJacksonFromStringImpl(fileContent, extension))
                    .execute(endpointsToRemove);

            // 2 — Zipper le fichier nettoyé
            String filename = "swagger-cleared." + extensionParam.toLowerCase();
            byte[] zipBytes = ZipUtils.buildFromNode(clearedNode, filename);

            exchange.getResponseHeaders().set("Content-Disposition",
                    "attachment; filename=\"swagger-cleared.zip\"");
            RestUtils.sendBytes(exchange, 200, "application/zip", zipBytes);

            log.info("REST ClearEndpoints — {} endpoint(s) supprimé(s), ZIP retourné ({} octets)",
                    endpointsToRemove.size(), zipBytes.length);

        } catch (IllegalArgumentException e) {
            log.warn("REST ClearEndpoints — paramètre invalide : {}", e.getMessage());
            RestUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            log.error("REST ClearEndpoints — erreur inattendue", e);
            RestUtils.sendError(exchange, 500, "Erreur interne : " + e.getMessage());
        }
    }
}
