package org.valle.present.rest;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du {@link SwaggerUiHandler}.
 *
 * <p>La ressource classpath {@code swagger-doc/index.html} est fournie par
 * {@code src/test/resources/swagger-doc/index.html} (stub de la page générée
 * par la tâche Gradle {@code openApiGenerate}).
 */
@ExtendWith(MockitoExtension.class)
class SwaggerUiHandlerTest {

    @Mock HttpExchange exchange;

    SwaggerUiHandler handler = new SwaggerUiHandler();

    final Headers               responseHeaders = new Headers();
    final ByteArrayOutputStream responseBody    = new ByteArrayOutputStream();

    @BeforeEach
    void setUpExchange() throws IOException {
        lenient().when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        lenient().when(exchange.getResponseBody()).thenReturn(responseBody);
        lenient().doNothing().when(exchange).sendResponseHeaders(anyInt(), anyLong());
    }

    // =========================================================================
    // Comportement nominal
    // =========================================================================

    @Nested
    class NominalBehaviour {

        @Test
        void should_return_200_for_GET() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("GET");

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
        }

        @Test
        void should_return_html_content_type() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("GET");

            handler.handle(exchange);

            assertThat(responseHeaders.getFirst("Content-Type"))
                    .startsWith("text/html");
        }

        @Test
        void should_serve_generated_documentation_html() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("GET");

            handler.handle(exchange);

            // vérifie que la ressource classpath swagger-doc/index.html est bien servie
            String body = responseBody.toString();
            assertThat(body)
                    .contains("<!DOCTYPE html>")
                    .contains("Swagger Organiser API");
        }

        @Test
        void should_expose_constant_resource_path() {
            // garantit que le chemin classpath ne change pas sans mise à jour du build
            assertThat(SwaggerUiHandler.SWAGGER_DOC_RESOURCE_PATH)
                    .isEqualTo("/swagger-doc/index.html");
        }
    }

    // =========================================================================
    // Validation de la méthode HTTP
    // =========================================================================

    @Nested
    class HTTPMethodValidation {

        @Test
        void should_return_405_when_method_is_POST() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(405), anyLong());
        }

        @Test
        void should_return_405_when_method_is_PUT() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("PUT");

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(405), anyLong());
        }

        @Test
        void should_return_405_when_method_is_DELETE() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("DELETE");

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(405), anyLong());
        }
    }
}
