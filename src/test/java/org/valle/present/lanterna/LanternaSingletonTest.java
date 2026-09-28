package org.valle.present.lanterna;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanternaSingletonTest {

    @Test
    void should_return_the_same_instance_for_each_call() {
        assertThat(LanternaSingleton.getInstance())
                .isSameAs(LanternaSingleton.getInstance());
    }

    @Test
    void should_expose_a_shared_terminal_factory() {
        assertThat(LanternaSingleton.getInstance().getTerminal()).isNotNull();
    }
}
