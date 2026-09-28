package org.valle.present.rest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestServerTest {

    @Test
    void should_use_default_port_without_arguments() {
        assertThat(RestServer.parsePort(new String[0])).isEqualTo(RestServer.DEFAULT_PORT);
    }

    @Test
    void should_accept_a_positional_port() {
        assertThat(RestServer.parsePort(new String[]{"9090"})).isEqualTo(9090);
    }

    @Test
    void should_accept_port_option_aliases() {
        assertThat(RestServer.parsePort(new String[]{"-p", "9090"})).isEqualTo(9090);
        assertThat(RestServer.parsePort(new String[]{"--port", "9090"})).isEqualTo(9090);
    }

    @Test
    void should_reject_invalid_port_values() {
        assertThatThrownBy(() -> RestServer.parsePort(new String[]{"--port", "70000"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 et 65535");
        assertThatThrownBy(() -> RestServer.parsePort(new String[]{"not-a-port"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Port invalide");
    }

    @Test
    void should_reject_unknown_server_arguments() {
        assertThatThrownBy(() -> RestServer.parsePort(new String[]{"--host", "localhost"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usage serveur");
    }
}
