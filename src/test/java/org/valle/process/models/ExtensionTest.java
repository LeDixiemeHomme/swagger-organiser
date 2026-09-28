package org.valle.process.models;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionTest {

    @Test
    void should_detect_supported_file_extensions() {
        assertThat(Extension.getSwaggerFileExtension(new File("swagger.yml"))).isEqualTo(Extension.YML);
        assertThat(Extension.getSwaggerFileExtension(new File("swagger.yaml"))).isEqualTo(Extension.YAML);
        assertThat(Extension.getSwaggerFileExtension(new File("swagger.json"))).isEqualTo(Extension.JSON);
    }

    @Test
    void should_reject_unsupported_file_extensions() {
        assertThatThrownBy(() -> Extension.getSwaggerFileExtension(new File("swagger.txt")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("swagger.txt");
    }
}
