/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.azure.datafactory;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createDataAddress;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AzureDataFactoryTransferRequestValidatorTest {
    private static final DataFlowRequest.Builder REQUEST = createRequest(AzureBlobStoreSchema.TYPE);

    private final Map<String, String> sourceProperties = TestFunctions.sourceProperties();
    private final Map<String, String> destinationProperties = TestFunctions.destinationProperties();
    private final DataAddress.Builder source = createDataAddress(AzureBlobStoreSchema.TYPE);
    private final DataAddress.Builder destination = createDataAddress(AzureBlobStoreSchema.TYPE);
    private final String extraKey = "test-extra-key";
    private final String extraValue = "test-extra-value";
    AzureDataFactoryTransferRequestValidator validator = new AzureDataFactoryTransferRequestValidator();

    static Collection<String> sourcePropertyKeys() {
        return TestFunctions.sourceProperties().keySet();
    }

    private static Stream<Arguments> canHandleArguments() {
        return Stream.of(
                arguments("Invalid source and valid destination", "Invalid source", AzureBlobStoreSchema.TYPE, false),
                arguments("Valid source and invalid destination", AzureBlobStoreSchema.TYPE, "Invalid destination", false),
                arguments("Invalid source and destination", "Invalid source", "Invalid destination", false),
                arguments("Valid source and destination", AzureBlobStoreSchema.TYPE, AzureBlobStoreSchema.TYPE, true)
        );
    }

    private static Collection<String> destinationPropertyKeys() {
        return TestFunctions.destinationProperties().keySet();
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("canHandleArguments")
    void canHandle_onResult(String ignoredName, String sourceType, String destinationType, boolean expected) {
        // Arrange
        var source = createDataAddress(sourceType);
        var destination = createDataAddress(destinationType);
        var request = DataFlowRequest.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(source.build())
                .destinationDataAddress(destination.build());
        // Act & Assert
        assertThat(validator.canHandle(request.build())).isEqualTo(expected);
    }

    @Test
    void validate_whenRequestValid_succeeds() {
        var result = validator.validate(REQUEST
                .sourceDataAddress(source.properties(TestFunctions.sourceProperties()).build())
                .destinationDataAddress(destination.properties(destinationProperties).build())
                .build());
        assertThat(result.succeeded()).withFailMessage(result::getFailureDetail).isTrue();
    }

    @Test
    void validate_whenExtraSourceProperty_fails() {
        assertThat(validator.validate(REQUEST
                .sourceDataAddress(source.properties(sourceProperties).property(extraKey, extraValue).build())
                .destinationDataAddress(destination.properties(destinationProperties).build())
                .build()).failed()).isTrue();
    }

    @Test
    void validate_whenExtraDestinationProperty_fails() {
        assertThat(validator.validate(REQUEST
                .sourceDataAddress(source.properties(sourceProperties).build())
                .destinationDataAddress(destination.properties(destinationProperties).property(extraKey, extraValue).build())
                .build()).failed()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("sourcePropertyKeys")
    void validate_whenMissingSourceProperty_fails(String property) {
        var src = new HashMap<>(sourceProperties);
        src.remove(property);
        assertThat(validator.validate(REQUEST
                .sourceDataAddress(source.properties(src).build())
                .destinationDataAddress(destination.properties(destinationProperties).build())
                .build()).failed()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("destinationPropertyKeys")
    void validate_whenMissingDestinationProperty_fails(String property) {
        var dest = new HashMap<>(destinationProperties);
        dest.remove(property);
        assertThat(validator.validate(REQUEST
                .sourceDataAddress(source.properties(sourceProperties).build())
                .destinationDataAddress(destination.properties(dest).build())
                .build()).failed()).isTrue();
    }
}
