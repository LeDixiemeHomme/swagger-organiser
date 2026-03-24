package org.valle.present.rest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.ClearEndpointOnDemandImpl;
import org.valle.process.models.EndPoint;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.fromstring.jackson.GetSwaggerNodeJacksonFromStringImpl;
import org.valle.utils.JacksonUtils;

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
 * de composants qui leur sont exclusivement associés, puis retourne le fichier nettoyé dans le
 * même format que l'entrée.
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
 * <p>Le fichier Swagger peut être envoyé de deux façons :
 * <ul>
 *   <li><b>multipart/form-data</b> — champ nommé {@code file} (recommandé, compatible Bruno/curl {@code -F})</li>
 *   <li><b>Corps brut</b> — {@code application/octet-stream} ou sans Content-Type (compatible curl {@code --data-binary})</li>
 * </ul>
 *
 * <h3>Réponse</h3>
 * <ul>
 *   <li>{@code 200 OK} — fichier Swagger nettoyé, {@code Content-Type: application/yaml} ou {@code application/json}</li>
 *   <li>{@code 400 Bad Request} — paramètre manquant, endpoint introuvable ou format invalide</li>
 *   <li>{@code 405 Method Not Allowed} — méthode HTTP autre que POST</li>
 *   <li>{@code 500 Internal Server Error} — erreur inattendue côté serveur</li>
 * </ul>
 *
 * <h3>Exemples</h3>
 * <pre>
 * # Supprimer un endpoint (curl multipart)
 * curl -X POST \
 *   "http://localhost:8080/swagger/clear-endpoints?extension=yml&endpoints=get:/cadh/v1/operations" \
 *   -F "file=@swagger.yml" --output cleared.yml
 *
 * # Supprimer plusieurs endpoints
 * curl -X POST \
 *   "http://localhost:8080/swagger/clear-endpoints?extension=yml&endpoints=get:/cadh/v1/operations,post:/cadh/v1/operations/{id}/documents" \
 *   -F "file=@swagger.yml" --output cleared.yml
 * </pre>
 *
 * @see DecomposeHandler pour décomposer le swagger nettoyé en plusieurs fichiers
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

            String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
            SwaggerNode clearedNode = new ClearEndpointOnDemandImpl(
                    new GetSwaggerNodeJacksonFromStringImpl(fileContent, extension))
                    .execute(endpointsToRemove);

            byte[] result = JacksonUtils.writeValueAsBytes(clearedNode);
            RestUtils.sendBytes(exchange, 200, RestUtils.resolveContentType(extension), result);

            log.info("REST ClearEndpoints — {} endpoint(s) supprimé(s), {} octets retournés",
                    endpointsToRemove.size(), result.length);

        } catch (IllegalArgumentException e) {
            log.warn("REST ClearEndpoints — paramètre invalide : {}", e.getMessage());
            RestUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            log.error("REST ClearEndpoints — erreur inattendue", e);
            RestUtils.sendError(exchange, 500, "Erreur interne : " + e.getMessage());
        }
    }
}
