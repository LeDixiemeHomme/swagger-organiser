package org.valle.present.rest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

/**
 * Handler REST — {@code GET /swagger-ui}
 *
 * <p>Sert la page HTML de documentation générée par
 * <strong>openapi-generator</strong> (générateur {@code html2}) à partir
 * de {@code openapi.yml}.
 *
 * <p>Le fichier {@code swagger-doc/index.html} est produit par la tâche
 * Gradle {@code openApiGenerate} et inclus dans le JAR via
 * {@code processResources}.
 *
 * <h3>Méthode HTTP</h3>
 * {@code GET} — toute autre méthode retourne {@code 405 Method Not Allowed}.
 *
 * <h3>Réponse</h3>
 * <ul>
 *   <li>{@code 200 OK} — page HTML ({@code Content-Type: text/html})</li>
 *   <li>{@code 405 Method Not Allowed} — méthode HTTP autre que GET</li>
 *   <li>{@code 500 Internal Server Error} — ressource introuvable ou illisible</li>
 * </ul>
 *
 * <h3>Exemple</h3>
 * <pre>
 * curl http://localhost:8080/swagger-ui
 * # ou ouvrir http://localhost:8080/swagger-ui dans un navigateur
 * </pre>
 */
@Slf4j
public class SwaggerUiHandler implements HttpHandler {

    /** Chemin de la ressource classpath générée par {@code openApiGenerate}. */
    static final String SWAGGER_DOC_RESOURCE_PATH = "/swagger-doc/index.html";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            RestUtils.sendError(exchange, 405, "Méthode non supportée — utilisez GET.");
            return;
        }
        try (InputStream is = SwaggerUiHandler.class.getResourceAsStream(SWAGGER_DOC_RESOURCE_PATH)) {
            if (is == null) {
                log.error("SwaggerUi — documentation introuvable : {}", SWAGGER_DOC_RESOURCE_PATH);
                RestUtils.sendError(exchange, 500, "Documentation Swagger introuvable — relancez ./gradlew openApiGenerate.");
                return;
            }
            byte[] html = is.readAllBytes();
            RestUtils.sendBytes(exchange, 200, "text/html; charset=UTF-8", html);
            log.debug("SwaggerUi — page servie ({} octets)", html.length);
        } catch (Exception e) {
            log.error("SwaggerUi — erreur lors du chargement de la page", e);
            RestUtils.sendError(exchange, 500, "Erreur interne : " + e.getMessage());
        }
    }
}
