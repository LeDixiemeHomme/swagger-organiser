package org.valle.present.rest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

final class HealthHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            RestUtils.sendError(exchange, 405, "Méthode non supportée — utilisez GET.");
            return;
        }

        RestUtils.sendBytes(exchange, 200, "application/json; charset=UTF-8",
                "{\"status\":\"UP\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
