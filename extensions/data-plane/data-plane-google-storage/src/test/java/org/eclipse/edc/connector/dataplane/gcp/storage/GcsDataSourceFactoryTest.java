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

package org.eclipse.edc.connector.dataplane.gcp.storage;

import org.eclipse.edc.gcp.storage.GcsStoreSchema;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GcsDataSourceFactoryTest {

    Monitor monitor = mock(Monitor.class);

    private final GcsDataSourceFactory factory =
            new GcsDataSourceFactory(monitor);

    @Test
    void canHandle_returnsTrueWhenExpectedType() {
        var dataAddress = TestFunctions.createDataAddress(GcsStoreSchema.TYPE)
                .build();
        var result = factory.canHandle(TestFunctions.createRequest(dataAddress));

        assertThat(result).isTrue();
    }

    @Test
    void canHandle_returnsFalseWhenUnexpectedType() {
        var dataAddress = TestFunctions.createDataAddress("Not Google Storage")
                .build();

        var result = factory.canHandle(TestFunctions.createRequest(dataAddress));

        assertThat(result).isFalse();
    }

    @Test
    void validate_ShouldSucceedIfPropertiesAreValid() {
        var source = TestFunctions.createDataAddress(GcsStoreSchema.TYPE)
                .property(GcsStoreSchema.BUCKET_NAME, "validBucketName")
                .property(GcsStoreSchema.BLOB_NAME, "validBlobName")
                .build();

        var request = TestFunctions.createRequest(source);

        var result = factory.validateRequest(request);

        assertThat(result.succeeded()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void validate_shouldFailIfPropertiesAreMissing(String bucketName, String blobName) {
        var source = TestFunctions.createDataAddress(GcsStoreSchema.TYPE)
                .property(GcsStoreSchema.BUCKET_NAME, bucketName)
                .property(GcsStoreSchema.BLOB_NAME, blobName)
                .build();

        var request = TestFunctions.createRequest(source);

        var result = factory.validateRequest(request);

        assertThat(result.failed()).isTrue();
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of("validBucketName", ""),
                Arguments.of(" ", "validBlobName"),
                Arguments.of("", " ")
        );
    }
}