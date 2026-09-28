package org.valle.present.rest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.valle.process.exceptions.EndPointNotFoundException;

import java.io.IOException;

/**
 * Common HTTP plumbing for the REST endpoints.
 *
 * <p>Handlers only implement their operation-specific orchestration. Method validation and the
 * public error contract stay in one place so every endpoint behaves consistently.</p>
 */
abstract class AbstractRestHandler implements HttpHandler {

    private final String allowedMethod;
    private final String operationName;

    protected AbstractRestHandler(String allowedMethod, String operationName) {
        this.allowedMethod = allowedMethod;
        this.operationName = operationName;
    }

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        if (!allowedMethod.equalsIgnoreCase(exchange.getRequestMethod())) {
            RestUtils.sendError(exchange, 405,
                    "Méthode non supportée — utilisez " + allowedMethod + ".");
            return;
        }

        try {
            handleRequest(exchange);
        } catch (IllegalArgumentException | EndPointNotFoundException e) {
            RestUtils.logInvalidRequest(operationName, e);
            RestUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            RestUtils.logInternalError(operationName, e);
            RestUtils.sendError(exchange, 500, "Erreur interne : " + safeMessage(e));
        }
    }

    protected abstract void handleRequest(HttpExchange exchange) throws Exception;

    private static String safeMessage(Exception exception) {
        return exception.getMessage() == null ? "Une erreur inattendue est survenue." : exception.getMessage();
    }
}
