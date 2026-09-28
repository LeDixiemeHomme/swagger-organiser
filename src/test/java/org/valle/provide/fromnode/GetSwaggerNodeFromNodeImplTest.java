package org.valle.provide.fromnode;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;

import static org.assertj.core.api.Assertions.assertThat;

class GetSwaggerNodeFromNodeImplTest {

    @Test
    void should_return_the_provided_swagger_node() {
        SwaggerNode expected = SwaggerNode.builder()
                .node(new ObjectMapper().createObjectNode().put("openapi", "3.0.0"))
                .extension(Extension.JSON)
                .build();

        assertThat(new GetSwaggerNodeFromNodeImpl(expected).provide()).isSameAs(expected);
    }
}
