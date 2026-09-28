package org.valle.present.rest;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.valle.process.MergeSwagger;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergeHandlerTest {

    @Mock
    HttpExchange exchange;
    @Mock
    MergeSwagger mergeSwagger;
    @Mock
    SwaggerNode mergedNode;

    private final Headers responseHeaders = new Headers();
    private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
    private final MergeHandler handler = new MergeHandler();

    @BeforeEach
    void setUp() throws IOException {
        lenient().when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        lenient().when(exchange.getResponseBody()).thenReturn(responseBody);
        lenient().doNothing().when(exchange).sendResponseHeaders(anyInt(), anyLong());
        handler.mergeFactory = (mainFile, baseDir) -> mergeSwagger;
        handler.zipBuildFactory = (node, filename) -> new byte[]{0x50, 0x4B};
        lenient().when(mergeSwagger.execute()).thenReturn(mergedNode);
    }

    @Test
    void should_return_405_for_non_post_requests() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(405), anyLong());
    }

    @Test
    void should_return_400_when_extension_is_missing() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/merge"));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
    }

    @Test
    void should_return_400_when_zip_does_not_contain_main_file() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/merge?extension=yml"));
        when(exchange.getRequestHeaders()).thenReturn(new Headers());
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
    }

    @Test
    void should_return_400_for_unknown_extension() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/merge?extension=xml"));
        when(exchange.getRequestHeaders()).thenReturn(new Headers());
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[]{1}));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
    }

    @Test
    void should_merge_a_valid_zip_and_return_the_result_archive() throws Exception {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/merge?extension=json"));
        when(exchange.getRequestHeaders()).thenReturn(new Headers());
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(zipWithMainFile()));

        handler.handle(exchange);

        verify(mergeSwagger).execute();
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertThat(responseHeaders.getFirst("Content-Type")).isEqualTo("application/zip");
        assertThat(responseHeaders.getFirst("Content-Disposition"))
                .isEqualTo("attachment; filename=\"swagger-merged.zip\"");
        assertThat(responseBody.toByteArray()).containsExactly(0x50, 0x4B);
    }

    @Test
    void should_return_400_when_request_body_is_not_a_zip() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/merge?extension=json"));
        when(exchange.getRequestHeaders()).thenReturn(new Headers());
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
    }

    @Test
    void should_reject_zip_slip_entries() throws Exception {
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(URI.create("/merge?extension=json"));
        when(exchange.getRequestHeaders()).thenReturn(new Headers());
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(zipWithEntry("../outside.json")));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(400), anyLong());
    }

    private static byte[] zipWithMainFile() throws IOException {
        return zipWithEntry("main.json");
    }

    private static byte[] zipWithEntry(String entryName) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write("{\"openapi\":\"3.0.0\"}".getBytes());
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
