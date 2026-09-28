package org.valle.present.logger;

import org.junit.jupiter.api.Test;
import org.valle.process.models.EndPoint;

import java.util.Set;

class ShowEndpointsLoggerImplTest {

    @Test
    void should_display_an_empty_endpoint_set_without_failing() {
        new ShowEndpointsLoggerImpl().display(Set.of());
    }

    @Test
    void should_display_each_endpoint_without_failing() {
        new ShowEndpointsLoggerImpl().display(Set.of(
                EndPoint.builder().method("get").path("/users").build(),
                EndPoint.builder().method("post").path("/users").build()
        ));
    }
}
