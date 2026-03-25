package org.valle.present.rest;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.valle.process.DecomposeSwagger;
import org.valle.process.models.Extension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecomposeHandlerTest {

    @Mock
    HttpExchange exchange;
    @Mock
    DecomposeSwagger mockDecompose;

    DecomposeHandler handler = new DecomposeHandler();

    final Headers responseHeaders = new Headers();
    final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();

    @BeforeEach
    void setUpExchange() throws IOException {
        lenient().when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        lenient().when(exchange.getResponseBody()).thenReturn(responseBody);
        lenient().doNothing().when(exchange).sendResponseHeaders(anyInt(), anyLong());
    }

    // =========================================================================
    // Orchestration — vérification de l'ordre et des paramètres des appels
    // =========================================================================

    @Nested
    class Orchestration {

        // Champs pour capturer les arguments reçus par la factory injectée
        String capturedContent;
        Extension capturedExtension;

        @BeforeEach
        void injectFactory() {
            handler.decomposeFactory = (content, ext) -> {
                capturedContent = content;
                capturedExtension = ext;
                return mockDecompose;
            };
            // ZipBuildFactory stubbée pour éviter d'appeler ZipUtils réel
            handler.zipBuildFactory = decomposed -> new byte[]{0x50, 0x4B};
        }

        private void givenPostRequest(String query, String fileContent) throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestURI()).thenReturn(URI.create("/swagger/decompose?" + query));
            Headers requestHeaders = new Headers();
            when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
            when(exchange.getRequestBody())
                    .thenReturn(new ByteArrayInputStream(fileContent.getBytes()));
        }

        @Test
        void should_call_execute_on_decompose_service() throws IOException {
            givenPostRequest("extension=yml", "openapi: 3.0.0");

            handler.handle(exchange);

            verify(mockDecompose).execute();
        }

        @Test
        void should_pass_yml_extension_to_decompose_factory() throws IOException {
            givenPostRequest("extension=yml", "openapi: 3.0.0");

            handler.handle(exchange);

            assertThat(capturedExtension).isEqualTo(Extension.YML);
        }

        @Test
        void should_pass_yaml_extension_to_decompose_factory() throws IOException {
            givenPostRequest("extension=yaml", "openapi: 3.0.0");

            handler.handle(exchange);

            assertThat(capturedExtension).isEqualTo(Extension.YAML);
        }

        @Test
        void should_pass_json_extension_to_decompose_factory() throws IOException {
            givenPostRequest("extension=json", "{\"openapi\":\"3.0.0\"}");

            handler.handle(exchange);

            assertThat(capturedExtension).isEqualTo(Extension.JSON);
        }

        @Test
        void should_pass_raw_file_content_to_decompose_factory() throws IOException {
            String yamlContent = "openapi: 3.0.0\ninfo:\n  title: Test API\n  version: 1.0.0";
            givenPostRequest("extension=yml", yamlContent);

            handler.handle(exchange);

            assertThat(capturedContent).isEqualTo(yamlContent);
        }

        @Test
        void should_return_200_with_zip_content_type() throws IOException {
            givenPostRequest("extension=yml", "openapi: 3.0.0");

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            assertThat(responseHeaders.getFirst("Content-Type")).isEqualTo("application/zip");
        }

        @Test
        void should_set_content_disposition_with_decomposed_filename() throws IOException {
            givenPostRequest("extension=yml", "openapi: 3.0.0");

            handler.handle(exchange);

            assertThat(responseHeaders.getFirst("Content-Disposition"))
                    .isEqualTo("attachment; filename=\"swagger-decomposed.zip\"");
        }
    }

    // =========================================================================
    // Validation de la méthode HTTP
    // =========================================================================

    @Nested
    class HTTPMethodValidation {

        @Test
        void should_return_405_when_method_is_GET() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("GET");

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

    // =========================================================================
    // Validation des paramètres de la requête
    // =========================================================================

    @Nested
    class ParameterValidation {

        @Test
        void should_return_400_when_extension_is_missing() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestURI()).thenReturn(URI.create("/swagger/decompose"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
        }

        @Test
        void should_return_400_when_extension_is_blank() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestURI()).thenReturn(URI.create("/swagger/decompose?extension="));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
        }

        @Test
        void should_return_400_when_body_is_empty() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestURI()).thenReturn(URI.create("/swagger/decompose?extension=yml"));
            when(exchange.getRequestHeaders()).thenReturn(new Headers());
            when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[0]));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
        }

        @Test
        void should_return_400_when_extension_is_unknown() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestURI()).thenReturn(URI.create("/swagger/decompose?extension=xml"));
            when(exchange.getRequestHeaders()).thenReturn(new Headers());
            when(exchange.getRequestBody())
                    .thenReturn(new ByteArrayInputStream("content".getBytes()));

            handler.handle(exchange);

            // IllegalArgumentException levée par Extension.valueOf("XML")
            verify(exchange).sendResponseHeaders(eq(400), anyLong());
        }
    }
}

