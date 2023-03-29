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

package org.eclipse.edc.connector.api.management.contractnegotiation.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NegotiationInitiateRequestDtoValidationTest {
    private Validator validator;

    @NotNull
    private static ContractOfferDescription validOffer() {
        return ContractOfferDescription.Builder.newInstance()
                .offerId("offerId")
                .assetId("assetid")
                .policy(null)
                .build();
    }

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ProviderConsumerProvider.class)
    void validate_validDto(String consumer, String provider) {
        var offerDescription = validOffer();
        var dto = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorAddress("connectorAddress")
                .connectorId("connectorId")
                .protocol("protocol")
                .consumerId(consumer)
                .providerId(provider)
                .offer(offerDescription)
                .build();

        assertThat(validator.validate(dto)).isEmpty();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidArgumentsProvider.class)
    void validate_invalidStringProps(String connectorAddress, String protocol, String connectorId, ContractOfferDescription description) {
        var dto = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorAddress(connectorAddress)
                .connectorId(connectorId)
                .protocol(protocol)
                .offer(description)
                .build();

        assertThat(validator.validate(dto)).hasSizeGreaterThanOrEqualTo(1);
    }

    private static class InvalidArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of("", "ids-multipart", "connector-id", validOffer()),
                    Arguments.of("https://connector.com", "", "connector-id", validOffer()),
                    Arguments.of("https://connector.com", "ids-multipart", "connector-id", null),
                    Arguments.of("https://connector.com", "ids-multipart", "", validOffer())
            );
        }
    }

    private static class ProviderConsumerProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of("urn:connector:consumer", "urn:connector:provider"),
                    Arguments.of("", "provider"),
                    Arguments.of(null, "provider"),
                    Arguments.of("consumer", "provider"),
                    Arguments.of("consumer", ""),
                    Arguments.of("consumer", null)
            );
        }
    }
}
