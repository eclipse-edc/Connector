/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.api.query;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuerySpecDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void defaultQuerySpecIsValid() {
        var querySpec = QuerySpecDto.Builder.newInstance().build();

        var result = validator.validate(querySpec);

        assertThat(result).isEmpty();
    }

    @Test
    void limitShouldBeGreaterThanZero() {
        var querySpec = QuerySpecDto.Builder.newInstance().limit(0).build();

        var result = validator.validate(querySpec);

        assertThat(result).isNotEmpty();
    }

    @Test
    void offsetShouldBeGreaterOrEqualThanZero() {
        var querySpec = QuerySpecDto.Builder.newInstance().offset(-1).build();

        var result = validator.validate(querySpec);

        assertThat(result).isNotEmpty();
    }

    @Test
    void filterShouldNotBeBlank() {
        var querySpec = QuerySpecDto.Builder.newInstance().filter("  ").build();

        var result = validator.validate(querySpec);

        assertThat(result).isNotEmpty();
    }

    @Test
    void sortFieldShouldNotBeBlank() {
        var querySpec = QuerySpecDto.Builder.newInstance().sortField("  ").build();

        var result = validator.validate(querySpec);

        assertThat(result).isNotEmpty();
    }
}
