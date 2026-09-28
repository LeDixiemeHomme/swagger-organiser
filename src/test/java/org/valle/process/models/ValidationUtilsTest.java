package org.valle.process.models;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.valle.process.models.validation.ValidationUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationUtilsTest {

    @Test
    void should_return_no_violations_for_a_valid_endpoint() {
        assertThat(ValidationUtils.validate(
                EndPoint.builder().method("get").path("/users").build()))
                .isEmpty();
    }

    @Test
    void should_throw_when_an_endpoint_contains_null_values() {
        assertThatThrownBy(() -> ValidationUtils.validate(
                EndPoint.builder().method(null).path(null).build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("method")
                .hasMessageContaining("path");
    }
}
