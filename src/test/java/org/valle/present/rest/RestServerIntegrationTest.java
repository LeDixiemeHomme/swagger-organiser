package org.valle.present.rest;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class RestServerIntegrationTest {

    private HttpServer server;
    private HttpClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = RestServer.createServer(0);
        server.start();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void should_decompose_a_real_http_request_into_a_zip_archive() throws Exception {
        String swagger = """
                {
                  "openapi": "3.0.0",
                  "info": {"title": "Integration API", "version": "1.0.0"},
                  "paths": {
                    "/users": {
                      "get": {
                        "responses": {"200": {"description": "OK"}}
                      }
                    }
                  }
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + server.getAddress().getPort()
                        + "/decompose?extension=json"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(swagger))
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).contains("application/zip");
        assertThat(response.headers().firstValue("Content-Disposition"))
                .hasValueSatisfying(value -> assertThat(value).contains("Integration-API-decomposed.zip"));

        Set<String> entries = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(response.body()))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.add(entry.getName());
            }
        }

        assertThat(entries).contains("main.json", "paths/users.json");
    }

    @Test
    void should_serve_embedded_app_and_protect_resource_traversal() throws Exception {
        URI appUri = URI.create("http://localhost:" + server.getAddress().getPort() + "/app");
        HttpResponse<String> appResponse = client.send(
                HttpRequest.newBuilder(appUri).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(appResponse.statusCode()).isEqualTo(200);
        assertThat(appResponse.headers().firstValue("Content-Type")).hasValueSatisfying(
                value -> assertThat(value).startsWith("text/html"));
        assertThat(appResponse.body()).contains("Swagger Organiser", "/app/app.js");

        HttpResponse<String> traversalResponse = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + server.getAddress().getPort()
                                + "/app/%2e%2e/openapi.yml"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(traversalResponse.statusCode()).isEqualTo(404);
        assertThat(traversalResponse.headers().firstValue("Content-Type")).hasValueSatisfying(
                value -> assertThat(value).startsWith("application/json"));
        assertThat(traversalResponse.body()).contains("\"code\":\"NOT_FOUND\"");
    }
}
