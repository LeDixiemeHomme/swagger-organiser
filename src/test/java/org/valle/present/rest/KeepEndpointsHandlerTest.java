package org.valle.present.rest;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.valle.process.KeepEndpointOnDemand;
import org.valle.process.models.EndPoint;
import org.valle.process.models.Extension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeepEndpointsHandlerTest {

    @Mock
    HttpExchange exchange;
    @Mock
    KeepEndpointOnDemand mockKeep;

    KeepEndpointsHandler handler = new KeepEndpointsHandler();

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
    class NominalBehaviour {

        String capturedContent;
        Extension capturedExtension;

        @BeforeEach
        void injectFactory() {
            handler.keepFactory = (content, ext) -> {
                capturedContent = content;
                capturedExtension = ext;
                return mockKeep;
            };
            handler.zipBuildFactory = (node, filename) -> new byte[]{0x50, 0x4B};
        }

        private void givenPostRequest(String query, String fileContent) throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestURI()).thenReturn(URI.create("/keep-endpoints?" + query));
            when(exchange.getRequestHeaders()).thenReturn(new Headers());
            when(exchange.getRequestBody())
                    .thenReturn(new ByteArrayInputStream(fileContent.getBytes()));
        }

        @Test
        void should_call_execute_with_single_endpoint_to_keep() throws IOException {
            givenPostRequest("extension=yml&endpoints=get:/cadh/v1/operations", "openapi: 3.0.0");

            handler.handle(exchange);

            Set<EndPoint> expected = Set.of(
                    EndPoint.builder().method("get").path("/cadh/v1/operations").build()
            );
            verify(mockKeep).execute(expected);
        }

        @Test
        void should_call_execute_with_multiple_endpoints_to_keep() throws IOException {
            givenPostRequest("extension=yml&endpoints=get:/path1,post:/path2", "openapi: 3.0.0");

            handler.handle(exchange);

            Set<EndPoint> expected = Set.of(
                    EndPoint.builder().method("get").path("/path1").build(),
                    EndPoint.builder().method("post").path("/path2").build()
            );
            verify(mockKeep).execute(expected);
        }

        @Test
        void should_call_execute_with_trimmed_endpoints() throws IOException {
            givenPostRequest("extension=yml&endpoints=get:/path1,%20post:/path2", "openapi: 3.0.0");

            handler.handle(exchange);

            Set<EndPoint> expected = Set.of(
                    EndPoint.builder().method("get").path("/path1").build(),
                    EndPoint.builder().method("post").path("/path2").build()
            );
            verify(mockKeep).execute(expected);
        }

        @Test
        void should_call_execute_with_path_variables() throws IOException {
            givenPostRequest("extension=yml&endpoints=get:/path1,get:/profiling/%7Bprofiling_id%7D",
                    "openapi: 3.0.0");

            handler.handle(exchange);

            Set<EndPoint> expected = Set.of(
                    EndPoint.builder().method("get").path("/path1").build(),
                    EndPoint.builder().method("get").path("/profiling/{profiling_id}").build()
            );
            verify(mockKeep).execute(expected);
        }

        @Test
        void should_pass_yml_extension_to_keep_factory() throws IOException {
            givenPostRequest("extension=yml&endpoints=get:/path", "openapi: 3.0.0");

            handler.handle(exchange);

            assertThat(capturedExtension).isEqualTo(Extension.YML);
        }

        @Test
        void should_pass_json_extension_to_keep_factory() throws IOException {
            givenPostRequest("extension=json&endpoints=get:/path", "{\"openapi\":\"3.0.0\"}");

            handler.handle(exchange);

            assertThat(capturedExtension).isEqualTo(Extension.JSON);
        }

        @Test
        void should_pass_raw_file_content_to_keep_factory() throws IOException {
            String yamlContent = "openapi: 3.0.0\ninfo:\n  title: Test API\n  version: 1.0.0";
            givenPostRequest("extension=yml&endpoints=get:/path", yamlContent);

            handler.handle(exchange);

            assertThat(capturedContent).isEqualTo(yamlContent);
        }

        @Test
        void should_return_200_with_zip_content_type() throws IOException {
            givenPostRequest("extension=yml&endpoints=get:/path", "openapi: 3.0.0");
            lenient().when(mockKeep.execute(org.mockito.ArgumentMatchers.any()))
                    .thenReturn(null);

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(200), anyLong());
            assertThat(responseHeaders.getFirst("Content-Type")).isEqualTo("application/zip");
        }

        @Test
        void should_set_content_disposition_with_kept_filename() throws IOException {
            givenPostRequest("extension=yml&endpoints=get:/path", "openapi: 3.0.0");

            handler.handle(exchange);

            assertThat(responseHeaders.getFirst("Content-Disposition"))
                    .isEqualTo("attachment; filename=\"swagger-kept.zip\"");
        }

        @Test
        void should_use_extension_in_zip_internal_filename() throws IOException {
            String[] capturedFilename = new String[1];
            handler.zipBuildFactory = (node, filename) -> {
                capturedFilename[0] = filename;
                return new byte[]{0x50, 0x4B};
            };
            givenPostRequest("extension=json&endpoints=get:/path", "{\"openapi\":\"3.0.0\"}");

            handler.handle(exchange);

            assertThat(capturedFilename[0]).isEqualTo("swagger-kept.json");
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
            when(exchange.getRequestURI()).thenReturn(
                    URI.create("/keep-endpoints?endpoints=get:/path"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
        }

        @Test
        void should_return_400_when_extension_is_blank() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestURI()).thenReturn(
                    URI.create("/keep-endpoints?extension=&endpoints=get:/path"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
        }

        @Test
        void should_return_400_when_endpoints_is_missing() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestURI()).thenReturn(
                    URI.create("/keep-endpoints?extension=yml"));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
        }

        @Test
        void should_return_400_when_endpoints_is_blank() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestURI()).thenReturn(
                    URI.create("/keep-endpoints?extension=yml&endpoints="));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
        }

        @Test
        void should_return_400_when_body_is_empty() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestURI()).thenReturn(
                    URI.create("/keep-endpoints?extension=yml&endpoints=get:/path"));
            when(exchange.getRequestHeaders()).thenReturn(new Headers());
            when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[0]));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
        }

        @Test
        void should_return_400_when_extension_is_unknown() throws IOException {
            when(exchange.getRequestMethod()).thenReturn("POST");
            when(exchange.getRequestURI()).thenReturn(
                    URI.create("/keep-endpoints?extension=xml&endpoints=get:/path"));
            when(exchange.getRequestHeaders()).thenReturn(new Headers());
            when(exchange.getRequestBody())
                    .thenReturn(new ByteArrayInputStream("content".getBytes()));

            handler.handle(exchange);

            verify(exchange).sendResponseHeaders(eq(400), anyLong());
        }
    }
}

