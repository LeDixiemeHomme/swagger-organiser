package org.valle.process.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EndPointTest {

    @Test
    void should_parse_method_and_path() {
        assertThat(EndPoint.fromString("get:/users/{id}"))
                .isEqualTo(EndPoint.builder().method("get").path("/users/{id}").build());
    }

    @Test
    void should_preserve_colons_in_path() {
        assertThat(EndPoint.fromString("get:/users?url=https://example.test"))
                .isEqualTo(EndPoint.builder().method("get").path("/users?url=https://example.test").build());
    }

    @Test
    void should_reject_an_endpoint_without_separator() {
        assertThatThrownBy(() -> EndPoint.fromString("get"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
