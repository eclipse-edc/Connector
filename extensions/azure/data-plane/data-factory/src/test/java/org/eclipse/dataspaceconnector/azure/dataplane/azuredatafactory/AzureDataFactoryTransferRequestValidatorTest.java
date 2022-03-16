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

package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createDataAddress;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createRequest;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createSharedKey;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AzureDataFactoryTransferRequestValidatorTest {
    static final Faker FAKER = new Faker();

    DataAddress.Builder source = createDataAddress(AzureBlobStoreSchema.TYPE);
    DataAddress.Builder destination = createDataAddress(AzureBlobStoreSchema.TYPE);

    static Map<String, String> sourceProperties = Map.of(
            AzureBlobStoreSchema.ACCOUNT_NAME, createAccountName(),
            AzureBlobStoreSchema.CONTAINER_NAME, createContainerName(),
            AzureBlobStoreSchema.BLOB_NAME, createBlobName(),
            AzureBlobStoreSchema.SHARED_KEY, createSharedKey());

    static Map<String, String> destinationProperties = Map.of(
            AzureBlobStoreSchema.ACCOUNT_NAME, createAccountName(),
            AzureBlobStoreSchema.CONTAINER_NAME, createContainerName(),
            AzureBlobStoreSchema.SHARED_KEY, createSharedKey());

    static DataFlowRequest.Builder request = createRequest(AzureBlobStoreSchema.TYPE);

    static DataFlowRequest requestWithProperties = request
            .sourceDataAddress(createDataAddress(AzureBlobStoreSchema.TYPE).properties(sourceProperties).build())
            .destinationDataAddress(createDataAddress(AzureBlobStoreSchema.TYPE).properties(destinationProperties).build())
            .build();

    String extraKey = FAKER.lorem().word();
    String extraValue = FAKER.lorem().word();

    AzureDataFactoryTransferRequestValidator validator = new AzureDataFactoryTransferRequestValidator();

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("canHandleArguments")
    void canHandle_onResult(String ignoredName, String sourceType, String destinationType, boolean expected) {
        // Arrange
        var source = createDataAddress(sourceType);
        var destination = createDataAddress(destinationType);
        var request = DataFlowRequest.Builder.newInstance()
                .processId(FAKER.internet().uuid())
                .sourceDataAddress(source.build())
                .destinationDataAddress(destination.build());
        // Act & Assert
        assertThat(validator.canHandle(request.build())).isEqualTo(expected);
    }

    @Test
    void validate_whenRequestValid_succeeds() {
        assertThat(validator.validate(request
                .sourceDataAddress(source.properties(sourceProperties).build())
                .destinationDataAddress(destination.properties(destinationProperties).build())
                .build()).succeeded()).isTrue();
    }

    @Test
    void validate_whenExtraSourceProperty_fails() {
        assertThat(validator.validate(request
                .sourceDataAddress(source.properties(sourceProperties).property(extraKey, extraValue).build())
                .destinationDataAddress(destination.properties(destinationProperties).build())
                .build()).failed()).isTrue();
    }

    @Test
    void validate_whenExtraDestinationProperty_fails() {
        assertThat(validator.validate(request
                .sourceDataAddress(source.properties(sourceProperties).build())
                .destinationDataAddress(destination.properties(destinationProperties).property(extraKey, extraValue).build())
                .build()).failed()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("sourcePropertyKeys")
    void validate_whenMissingSourceProperty_fails(String property) {
        var src = new HashMap<>(sourceProperties);
        src.remove(property);
        assertThat(validator.validate(request
                .sourceDataAddress(source.properties(src).build())
                .destinationDataAddress(destination.properties(destinationProperties).build())
                .build()).failed()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("destinationPropertyKeys")
    void validate_whenMissingDestinationProperty_fails(String property) {
        var dest = new HashMap<>(destinationProperties);
        dest.remove(property);
        assertThat(validator.validate(request
                .sourceDataAddress(source.properties(sourceProperties).build())
                .destinationDataAddress(destination.properties(dest).build())
                .build()).failed()).isTrue();
    }

    static Collection<String> sourcePropertyKeys() {
        return sourceProperties.keySet();
    }

    static Collection<String> destinationPropertyKeys() {
        return destinationProperties.keySet();
    }

    static Stream<Arguments> canHandleArguments() {
        return Stream.of(
                arguments("Invalid source and valid destination", FAKER.lorem().word(), AzureBlobStoreSchema.TYPE, false),
                arguments("Valid source and invalid destination", AzureBlobStoreSchema.TYPE, FAKER.lorem().word(), false),
                arguments("Invalid source and destination", FAKER.lorem().word(), FAKER.lorem().word(), false),
                arguments("Valid source and destination", AzureBlobStoreSchema.TYPE, AzureBlobStoreSchema.TYPE, true)
        );
    }
}
