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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthHandlerTest {

    @Mock HttpExchange exchange;
    private final Headers headers = new Headers();
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private final HealthHandler handler = new HealthHandler();

    @BeforeEach
    void setUp() throws IOException {
        when(exchange.getResponseHeaders()).thenReturn(headers);
        when(exchange.getResponseBody()).thenReturn(body);
    }

    @Test
    void should_return_up_status_for_get() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(200, 15);
        assertThat(headers.getFirst("Content-Type")).isEqualTo("application/json; charset=UTF-8");
        assertThat(body.toString()).isEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    void should_reject_non_get_requests() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(org.mockito.ArgumentMatchers.eq(405), anyLong());
    }
}
