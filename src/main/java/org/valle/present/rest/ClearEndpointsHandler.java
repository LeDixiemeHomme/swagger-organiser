package org.valle.present.rest;

import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.ClearEndpointOnDemand;
import org.valle.process.ClearEndpointOnDemandImpl;
import org.valle.process.models.EndPoint;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.fromstring.jackson.GetSwaggerNodeJacksonFromStringImpl;
import org.valle.utils.ZipUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

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
public class ClearEndpointsHandler extends AbstractRestHandler {

    public ClearEndpointsHandler() {
        super("POST", "ClearEndpoints");
    }

    /** Crée le service de suppression d'endpoints à partir du contenu et de l'extension. */
    @FunctionalInterface
    interface ClearFactory {
        ClearEndpointOnDemand create(String content, Extension extension);
    }

    /** Construit l'archive ZIP à partir du nœud Swagger nettoyé et du nom de fichier. */
    @FunctionalInterface
    interface ZipBuildFactory {
        byte[] build(SwaggerNode node, String filename) throws IOException;
    }

    // Package-private pour injection dans les tests
    ClearFactory clearFactory = (content, ext) ->
            new ClearEndpointOnDemandImpl(new GetSwaggerNodeJacksonFromStringImpl(content, ext));

    ZipBuildFactory zipBuildFactory = ZipUtils::buildFromNode;

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        Map<String, String> parameters = RestUtils.parseQuery(exchange.getRequestURI().getQuery());
        RestUtils.requireQueryParameter(parameters, "extension",
                "Paramètre 'extension' manquant (json, yml, yaml).");
        String endpointsParam = RestUtils.requireQueryParameter(parameters, "endpoints",
                "Paramètre 'endpoints' manquant (ex: get:/path,post:/path2).");
        Set<EndPoint> endpointsToRemove = RestUtils.parseEndpoints(endpointsParam);
        SwaggerRequest request = RestUtils.readSwaggerRequest(exchange, parameters,
                "Le corps de la requête est vide — envoyez le fichier Swagger.");

        log.info("REST ClearEndpoints — extension={}, {} endpoint(s) à supprimer, {} octets",
                request.extension(), endpointsToRemove.size(), request.body().length);

        SwaggerNode clearedNode = clearFactory.create(request.content(), request.extension())
                .execute(endpointsToRemove);
        byte[] zipBytes = zipBuildFactory.build(clearedNode, request.outputFilename("swagger-cleared"));

        exchange.getResponseHeaders().set("Content-Disposition",
                "attachment; filename=\"swagger-cleared.zip\"");
        RestUtils.sendBytes(exchange, 200, "application/zip", zipBytes);

        log.info("REST ClearEndpoints — {} endpoint(s) supprimé(s), ZIP retourné ({} octets)",
                endpointsToRemove.size(), zipBytes.length);
    }
}
