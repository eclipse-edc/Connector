/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ContractDefinitionDtoValidationTest {

    private Validator validator;


    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidArgsProvider.class)
    void validate_invalidProperty(String id, String accessPolicyId, String contractPolicyId) {

        var crit = List.of(new Criterion("foo", "=", "bar"));

        var dto = ContractDefinitionDto.Builder.newInstance()
                .id(id)
                .accessPolicyId(accessPolicyId)
                .contractPolicyId(contractPolicyId)
                .criteria(crit)
                .build();
        assertThat(validator.validate(dto)).hasSize(1);
    }

    @Test
    void validate_nullCriteria() {
        var dto = ContractDefinitionDto.Builder.newInstance()
                .id("id")
                .accessPolicyId("accessPolicyId")
                .contractPolicyId("contractPolicyId")
                .criteria(null)
                .build();
        assertThat(validator.validate(dto)).hasSize(1);
    }

    private static class InvalidArgsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(null, "accessPolicy", "contractPolicy"),
                    Arguments.of("id", null, "contractPolicy"),
                    Arguments.of("id", "accessPolicy", null)
            );
        }
    }
}