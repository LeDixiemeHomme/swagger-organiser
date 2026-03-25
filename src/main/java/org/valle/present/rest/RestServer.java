package org.valle.present.rest;

import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Serveur REST léger basé sur {@link HttpServer} (JDK intégré).
 *
 * <p>Usage : {@code java -cp swagger-organiser-all.jar org.valle.present.rest.RestServer [port]}
 * <p>Port par défaut : 8080
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST /clear-endpoints} — supprime des endpoints, retourne le fichier nettoyé</li>
 *   <li>{@code POST /keep-endpoints}  — conserve uniquement les endpoints fournis, supprime les autres</li>
 *   <li>{@code POST /decompose}       — décompose le swagger, retourne une archive ZIP</li>
 * </ul>
 */
@Slf4j
public class RestServer {

    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/clear-endpoints", new ClearEndpointsHandler());
        server.createContext("/keep-endpoints",  new KeepEndpointsHandler());
        server.createContext("/decompose",        new DecomposeHandler());
        server.createContext("/swagger-ui",       new SwaggerUiHandler());
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor()); // Java 21 virtual threads
        server.start();

        log.info("Serveur REST démarré sur le port {}", port);
        log.info("  GET  http://localhost:{}/swagger-ui  ← Swagger UI (interface graphique)", port);
        log.info("  POST http://localhost:{}/clear-endpoints?extension=yml&endpoints=method:/path", port);
        log.info("  POST http://localhost:{}/keep-endpoints?extension=yml&endpoints=method:/path", port);
        log.info("  POST http://localhost:{}/decompose?extension=yml", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Arrêt du serveur REST...");
            server.stop(1);
        }));
    }
}
