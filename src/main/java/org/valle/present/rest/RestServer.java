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
 * ou {@code ... RestServer --port 9090}
 * <p>Port par défaut : 8080
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /app} — interface web vanilla embarquée dans le JAR</li>
 *   <li>{@code POST /clear-endpoints} — supprime des endpoints, retourne le fichier nettoyé</li>
 *   <li>{@code POST /keep-endpoints}  — conserve uniquement les endpoints fournis, supprime les autres</li>
 *   <li>{@code POST /decompose}       — décompose le swagger, retourne une archive ZIP</li>
 *   <li>{@code POST /merge}           — fusionne un swagger décomposé (ZIP) en un seul fichier</li>
 * </ul>
 */
@Slf4j
public class RestServer {

    static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws IOException {
        int port = parsePort(args);

        HttpServer server = createServer(port);
        server.start();

        log.info("Serveur REST démarré sur le port {}", port);
        log.info("  GET  http://localhost:{}/app  ← Interface web", port);
        log.info("  GET  http://localhost:{}/swagger-ui  ← Swagger UI (interface graphique)", port);
        log.info("  POST http://localhost:{}/clear-endpoints?extension=yml&endpoints=method:/path", port);
        log.info("  POST http://localhost:{}/keep-endpoints?extension=yml&endpoints=method:/path", port);
        log.info("  POST http://localhost:{}/decompose?extension=yml", port);
        log.info("  POST http://localhost:{}/merge?extension=yml", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Arrêt du serveur REST...");
            server.stop(1);
        }));
    }

    static HttpServer createServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/app",              new ClasspathResourceHandler("/app", "index.html"));
        server.createContext("/clear-endpoints", new ClearEndpointsHandler());
        server.createContext("/keep-endpoints",  new KeepEndpointsHandler());
        server.createContext("/decompose",        new DecomposeHandler());
        server.createContext("/merge",            new MergeHandler());
        server.createContext("/swagger-ui",       new SwaggerUiHandler());
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor()); // Java 21 virtual threads
        return server;
    }

    static int parsePort(String[] args) {
        if (args == null || args.length == 0) {
            return DEFAULT_PORT;
        }

        if (args.length == 1 && !args[0].equals("-p") && !args[0].equals("--port")) {
            return parsePortValue(args[0]);
        }

        if (args.length == 2 && (args[0].equals("-p") || args[0].equals("--port"))) {
            return parsePortValue(args[1]);
        }

        throw new IllegalArgumentException(
                "Usage serveur : server [port] ou server --port <port>.");
    }

    private static int parsePortValue(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Le port doit être compris entre 1 et 65535.");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port invalide : " + value, e);
        }
    }
}
