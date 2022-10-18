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

package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TransferRequestDtoValidationTest {
    private Validator validator;

    private static TransferType transferType() {
        return new TransferType();
    }

    private static DataAddress destination() {
        return DataAddress.Builder.newInstance().type("testtype").build();
    }

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidPropertiesProvider.class)
    void validate_invalidProperties(String id, String address, String contract, DataAddress destination, TransferType type, String protocol, String connectorId, String assetId) {
        var dto = TransferRequestDto.Builder.newInstance()
                .id(id)
                .assetId(assetId)
                .connectorAddress(address)
                .connectorId(connectorId)
                .contractId(contract)
                .dataDestination(destination)
                .transferType(type)
                .protocol(protocol)
                .build();

        assertThat(validator.validate(dto)).hasSize(1);
    }

    private static class InvalidPropertiesProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of("id", null, "contractId", destination(), transferType(), "ids-multipart", "connectorId", "assetId"),
                    Arguments.of("id", "connectorAddress", null, destination(), transferType(), "ids-multipart", "connectorId", "assetId"),
                    Arguments.of("id", "connectorAddress", "contractId", null, transferType(), "ids-multipart", "connectorId", "assetId"),
                    Arguments.of("id", "connectorAddress", "contractId", destination(), null, "ids-multipart", "connectorId", "assetId"),
                    Arguments.of("id", "connectorAddress", "contractId", destination(), transferType(), null, "connectorId", "assetId"),
                    Arguments.of("id", "connectorAddress", "contractId", destination(), transferType(), "ids-multipart", null, "assetId"),
                    Arguments.of("id", "connectorAddress", "contractId", destination(), transferType(), "ids-multipart", "connectorId", null)
            );
        }
    }
}