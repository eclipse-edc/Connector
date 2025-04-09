/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.runtime.core.validator;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectValidatorRegistryImplTest {

    private final JsonObjectValidatorRegistry registry = new JsonObjectValidatorRegistryImpl();

    @Test
    void validate_shouldSucceed_whenValidatorDoesNotExist() {
        var result = registry.validate("not-existent", createObjectBuilder().build());

        assertThat(result).isSucceeded();
    }

    @Test
    void validate_shouldExecuteRegisteredRule() {
        var validator = Mockito.<Validator<JsonObject>>mock();
        var failure = failure(violation("validation error", "path"));
        when(validator.validate(any())).thenReturn(failure);
        registry.register("type-name", validator);
        var input = createObjectBuilder().build();

        var result = registry.validate("type-name", input);

        assertThat(result).isFailed().satisfies(f ->
                assertThat(f.getViolations()).hasSize(1).first().matches(it -> it.message().equals("validation error")));
        verify(validator).validate(input);
    }

    @Test
    void validate_multipleRules_oneViolation() {
        var failingValidator = Mockito.<Validator<JsonObject>>mock();
        var failure = failure(violation("validation error", "path"));
        when(failingValidator.validate(any())).thenReturn(failure);

        var validator = Mockito.<Validator<JsonObject>>mock();
        when(validator.validate(any())).thenReturn(ValidationResult.success());

        registry.register("type-name", failingValidator);
        registry.register("type-name", validator);

        var result = registry.validate("type-name", createObjectBuilder().build());
        assertThat(result).isFailed().satisfies(f ->
                assertThat(f.getViolations()).hasSize(1).first().matches(it -> it.message().equals("validation error")));
        verify(validator).validate(any());
        verify(failingValidator).validate(any());
    }

    @Test
    void validate_multipleRules_multipleViolations() {
        var failingValidator1 = Mockito.<Validator<JsonObject>>mock();
        var failure = failure(violation("validation error 1", "path"));
        when(failingValidator1.validate(any())).thenReturn(failure);

        var failingValidator2 = Mockito.<Validator<JsonObject>>mock();
        var failure2 = failure(violation("validation error 2", "path"));
        when(failingValidator2.validate(any())).thenReturn(failure2);

        registry.register("type-name", failingValidator1);
        registry.register("type-name", failingValidator2);

        var result = registry.validate("type-name", createObjectBuilder().build());
        assertThat(result).isFailed().satisfies(f -> assertThat(f.getViolations()).hasSize(2)
                .anyMatch(it -> it.message().equals("validation error 1"))
                .anyMatch(it -> it.message().equals("validation error 2")));
        verify(failingValidator1).validate(any());
        verify(failingValidator2).validate(any());
    }
}
