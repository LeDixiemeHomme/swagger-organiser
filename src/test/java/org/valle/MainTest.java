package org.valle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    @Test
    void should_return_success_for_valid_cli_help() {
        assertThat(Main.executeCli(new String[]{"--help"})).isZero();
    }

    @Test
    void should_return_an_error_for_invalid_cli_arguments() {
        assertThat(Main.executeCli(new String[]{"--unknown-option"})).isNotZero();
    }
}
