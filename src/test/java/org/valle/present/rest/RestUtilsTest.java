package org.valle.present.rest;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.valle.process.models.EndPoint;
import org.valle.process.models.Extension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestUtilsTest {

    @Mock
    HttpExchange exchange;

    @Test
    void should_parse_query_parameters() {
        Map<String, String> result = RestUtils.parseQuery("extension=yml&endpoints=get:/users&flag");

        assertThat(result).containsEntry("extension", "yml")
                .containsEntry("endpoints", "get:/users")
                .doesNotContainKey("flag");
    }

    @Test
    void should_decode_query_parameter_values() {
        assertThat(RestUtils.parseQuery("endpoints=get:/users/%7Buser_id%7D").get("endpoints"))
                .isEqualTo("get:/users/{user_id}");
    }

    @Test
    void should_return_empty_parameters_for_blank_query() {
        assertThat(RestUtils.parseQuery(null)).isEmpty();
        assertThat(RestUtils.parseQuery("")).isEmpty();
    }

    @Test
    void should_parse_endpoint_lists_with_whitespace() {
        assertThat(RestUtils.parseEndpoints(" get:/users, post:/orders "))
                .containsExactlyInAnyOrder(
                        EndPoint.builder().method("get").path("/users").build(),
                        EndPoint.builder().method("post").path("/orders").build());
    }

    @Test
    void should_resolve_supported_content_types() {
        assertThat(RestUtils.resolveContentType(Extension.JSON)).isEqualTo("application/json");
        assertThat(RestUtils.resolveContentType(Extension.YML)).isEqualTo("application/yaml");
        assertThat(RestUtils.resolveContentType(Extension.YAML)).isEqualTo("application/yaml");
    }

    @Test
    void should_read_raw_request_body() throws IOException {
        byte[] body = "openapi: 3.0.0".getBytes(StandardCharsets.UTF_8);
        Headers headers = new Headers();
        when(exchange.getRequestHeaders()).thenReturn(headers);
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(body));

        assertThat(RestUtils.readFileBytes(exchange)).isEqualTo(body);
    }

    @Test
    void should_extract_file_part_from_multipart_request() throws IOException {
        String boundary = "test-boundary";
        String multipart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"swagger.yml\"\r\n"
                + "Content-Type: application/yaml\r\n\r\n"
                + "openapi: 3.0.0\r\n"
                + "--" + boundary + "--\r\n";
        Headers headers = new Headers();
        headers.set("Content-Type", "multipart/form-data; boundary=" + boundary);
        when(exchange.getRequestHeaders()).thenReturn(headers);
        when(exchange.getRequestBody()).thenReturn(
                new ByteArrayInputStream(multipart.getBytes(StandardCharsets.UTF_8)));

        assertThat(new String(RestUtils.readFileBytes(exchange), StandardCharsets.UTF_8))
                .isEqualTo("openapi: 3.0.0");
    }

    @Test
    void should_reject_multipart_without_boundary() throws IOException {
        Headers headers = new Headers();
        headers.set("Content-Type", "multipart/form-data");
        when(exchange.getRequestHeaders()).thenReturn(headers);
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[0]));

        assertThatThrownBy(() -> RestUtils.readFileBytes(exchange))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Boundary manquant");
    }

    @Test
    void should_send_error_as_a_structured_json_body() throws IOException {
        Headers responseHeaders = new Headers();
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);

        RestUtils.sendError(exchange, 400, "Entrée invalide");

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
        assertThat(responseHeaders.getFirst("Content-Type")).isEqualTo("application/json; charset=UTF-8");
        assertThat(responseBody.toString(StandardCharsets.UTF_8))
                .isEqualTo("{\"code\":\"INVALID_REQUEST\",\"message\":\"Entrée invalide\"}");
    }

    @Test
    void should_send_binary_response() throws IOException {
        Headers responseHeaders = new Headers();
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getResponseBody()).thenReturn(responseBody);
        byte[] body = {1, 2, 3};

        RestUtils.sendBytes(exchange, 200, "application/zip", body);

        verify(exchange).sendResponseHeaders(200, body.length);
        assertThat(responseHeaders.getFirst("Content-Type")).isEqualTo("application/zip");
        assertThat(responseBody.toByteArray()).containsExactly(body);
    }
}
