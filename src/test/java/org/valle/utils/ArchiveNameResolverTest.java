package org.valle.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveNameResolverTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void should_use_requested_name_and_operation_suffix() throws Exception {
        String name = ArchiveNameResolver.resolve(
                "nouveau-swagger",
                mapper.readTree("{}"),
                "decomposed",
                "swagger-decomposed");

        assertThat(name).isEqualTo("nouveau-swagger-decomposed");
    }

    @Test
    void should_use_info_title_when_name_is_not_provided() throws Exception {
        String name = ArchiveNameResolver.resolve(
                "",
                mapper.readTree("""
                        {"openapi":"3.0.1","info":{"title":"DECISEO-API"}}
                        """),
                "merged",
                "swagger-merged");

        assertThat(name).isEqualTo("DECISEO-API-merged");
    }

    @Test
    void should_keep_historical_name_when_title_is_missing() throws Exception {
        String name = ArchiveNameResolver.resolve(
                null,
                mapper.readTree("{}"),
                "cleared",
                "swagger-cleared");

        assertThat(name).isEqualTo("swagger-cleared");
    }

    @Test
    void should_sanitize_path_separators_and_special_characters() throws Exception {
        String name = ArchiveNameResolver.resolve(
                "../unsafe name",
                mapper.readTree("{}"),
                "kept",
                "swagger-kept");

        assertThat(name).isEqualTo("unsafe-name-kept");
    }
}
