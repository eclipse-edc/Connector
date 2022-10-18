/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.dataspaceconnector.gcp.dataplane.gcs;

import org.eclipse.dataspaceconnector.gcp.core.storage.GcsStoreSchema;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GcsDataSinkFactoryTest {

    private final GcsDataSinkFactory factory = new GcsDataSinkFactory(
            mock(ExecutorService.class),
            mock(Monitor.class),
            mock(Vault.class),
            new TypeManager()
    );

    @Test
    void canHandle_returnsTrueWhenExpectedType() {
        var dataAddress = DataAddress.Builder
                .newInstance()
                .type(GcsStoreSchema.TYPE)
                .build();

        var result = factory.canHandle(createRequest(dataAddress));

        assertThat(result).isTrue();
    }

    @Test
    void canHandle_returnsFalseWhenUnexpectedType() {
        var dataAddress = DataAddress.Builder
                .newInstance()
                .type("Not Google Storage")
                .build();

        var result = factory.canHandle(createRequest(dataAddress));

        assertThat(result).isFalse();
    }

    @Test
    void validate_ShouldSucceedIfPropertiesAreValid() {
        var source = DataAddress.Builder
                .newInstance()
                .type(GcsStoreSchema.TYPE)
                .property(GcsStoreSchema.BUCKET_NAME, "validBucketName")
                .property(GcsStoreSchema.BLOB_NAME, "validBlobName")
                .build();

        var request = createRequest(source);

        var result = factory.validate(request);

        assertThat(result.succeeded()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void validate_shouldFailIfPropertiesAreMissing(String bucketName) {
        var source = DataAddress.Builder
                .newInstance()
                .type(GcsStoreSchema.TYPE)
                .property(GcsStoreSchema.BUCKET_NAME, bucketName)
                .build();

        var request = createRequest(source);

        var result = factory.validate(request);

        assertThat(result.failed()).isTrue();
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of(" ")
        );
    }

    private DataFlowRequest createRequest(DataAddress destination) {
        var source = DataAddress.Builder
                .newInstance()
                .type(GcsStoreSchema.TYPE)
                .build();

        return DataFlowRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .build();
    }
}