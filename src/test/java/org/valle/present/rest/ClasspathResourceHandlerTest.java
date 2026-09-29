package org.valle.present.rest;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClasspathResourceHandlerTest {

    @Mock
    HttpExchange exchange;

    private final Headers responseHeaders = new Headers();
    private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
    private final ClasspathResourceHandler handler =
            new ClasspathResourceHandler("/app", "index.html");

    @BeforeEach
    void setUpExchange() throws IOException {
        lenient().when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        lenient().when(exchange.getResponseBody()).thenReturn(responseBody);
        lenient().doNothing().when(exchange).sendResponseHeaders(anyInt(), anyLong());
    }

    @Test
    void should_serve_index_for_app_root() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/app"));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertThat(responseHeaders.getFirst("Content-Type")).startsWith("text/html");
        assertThat(responseBody.toString())
                .contains("Swagger Organiser")
                .contains("preserve-comments");
    }

    @Test
    void should_serve_javascript_asset() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/app/app.js"));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertThat(responseHeaders.getFirst("Content-Type")).startsWith("text/javascript");
        assertThat(responseBody.toString())
                .contains("transform-form")
                .contains("\"preserve-comments\"");
    }

    @Test
    void should_serve_css_asset() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/app/styles.css"));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertThat(responseHeaders.getFirst("Content-Type")).isEqualTo("text/css; charset=UTF-8");
    }

    @Test
    void should_reject_traversal_before_classpath_lookup() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/app/%2e%2e/openapi.yml"));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(404), anyLong());
        assertThat(responseBody.toString()).contains("\"code\":\"NOT_FOUND\"");
    }

    @Test
    void should_return_not_found_for_unknown_resource() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/app/missing.js"));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(404), anyLong());
    }

    @Test
    void should_reject_paths_outside_the_configured_context() {
        assertThat(handler.resolveResourcePath("/other/index.html")).isNull();
        assertThat(handler.resolveResourcePath(null)).isNull();
        assertThat(handler.resolveResourcePath("/app/../openapi.yml")).isNull();
        assertThat(handler.resolveResourcePath("/app/folder/../app.js")).isNull();
    }

    @Test
    void should_reject_invalid_handler_configuration() {
        assertThatThrownBy(() -> new ClasspathResourceHandler("app", "index.html"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClasspathResourceHandler("/", "index.html"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClasspathResourceHandler("/app/..", "index.html"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClasspathResourceHandler("/app/", "index.html"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClasspathResourceHandler("/app", "../index.html"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClasspathResourceHandler("/app", "/index.html"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_non_get_requests_with_json_error() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(405), anyLong());
        assertThat(responseHeaders.getFirst("Content-Type")).startsWith("application/json");
    }
}
