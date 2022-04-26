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

package org.eclipse.dataspaceconnector.api.datamanagement.contractagreement.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ContractAgreementDtoValidationTest {

    private Validator validator;

    private static long now() {
        return Instant.now().toEpochMilli();
    }

    private static long later(int i) {
        return Instant.now().plusMillis(i).toEpochMilli();
    }

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidArgsProvider.class)
    void validation_invalidProperty(String id, String assetId, String policyId, String consumerAgentId, String providerAgentId, long startDate, long endDate, long signingDate) {
        var agreement = ContractAgreementDto.Builder.newInstance()
                .assetId(assetId)
                .policyId(policyId)
                .consumerAgentId(consumerAgentId)
                .providerAgentId(providerAgentId)
                .id(id)
                .contractStartDate(startDate)
                .contractEndDate(endDate)
                .contractSigningDate(signingDate)
                .build();

        assertThat(validator.validate(agreement)).hasSizeGreaterThan(0);
    }

    @Test
    void validation_validDto() {
        var agreement = ContractAgreementDto.Builder.newInstance()
                .assetId("Asset")
                .policyId("policy")
                .consumerAgentId("cons")
                .providerAgentId("prov")
                .id("someId")
                .contractStartDate(15)
                .contractEndDate(18)
                .contractSigningDate(14)
                .build();

        assertThat(validator.validate(agreement)).isEmpty();
    }

    private static class InvalidArgsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(null, "asset", "policy", "consumer", "provider", now(), later(100), now()),
                    Arguments.of("id", null, "policy", "consumer", "provider", now(), later(100), now()),
                    Arguments.of("id", "asset", "policy", null, "provider", now(), later(10), now()),
                    Arguments.of("id", "asset", "policy", "consumer", null, 0, later(50), now()),
                    Arguments.of("id", "asset", "policy", "consumer", "provider", now(), 0, now()),
                    Arguments.of("id", "asset", "policy", "consumer", "provider", now(), later(100), later(200)), //invalid signing date -> after end date
                    Arguments.of("id", "asset", "policy", "consumer", "provider", now(), later(50), 0)
            );
        }
    }
}