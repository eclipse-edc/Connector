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

package org.eclipse.edc.connector.api.management.contractdefinition.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.eclipse.edc.api.model.CriterionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class ContractDefinitionUpdateDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValidArgsProvider.class)
    void validate_valid(String accessPolicyId, String contractPolicyId, List<CriterionDto> criteria) {
        var dto = ContractDefinitionUpdateDto.Builder.newInstance()
                .accessPolicyId(accessPolicyId)
                .contractPolicyId(contractPolicyId)
                .criteria(criteria)
                .build();

        var result = validator.validate(dto);

        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidArgsProvider.class)
    void validate_invalid(String accessPolicyId, String contractPolicyId, List<CriterionDto> criteria) {
        var dto = ContractDefinitionUpdateDto.Builder.newInstance()
                .accessPolicyId(accessPolicyId)
                .contractPolicyId(contractPolicyId)
                .criteria(criteria)
                .build();

        var result = validator.validate(dto);

        assertThat(result).hasSizeGreaterThan(0);
    }

    private static class ValidArgsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            var criterion = CriterionDto.Builder.newInstance().operandLeft("foo").operator("=").operandRight("bar").build();
            return Stream.of(
                    Arguments.of("accessPolicy", "contractPolicy", List.of(criterion)),
                    Arguments.of("accessPolicy", "contractPolicy", List.of(criterion)),
                    Arguments.of("accessPolicy", "contractPolicy", emptyList())
            );
        }
    }

    private static class InvalidArgsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(null, "contractPolicy", emptyList()),
                    Arguments.of("accessPolicy", null, emptyList()),
                    Arguments.of("accessPolicy", "contractPolicy", null),
                    Arguments.of("accessPolicy", "contractPolicy", List.of(CriterionDto.Builder.newInstance().build()))
            );
        }
    }
}
