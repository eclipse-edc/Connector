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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline;

import dev.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.azure.blob.core.adapter.BlobAdapter;
import org.eclipse.dataspaceconnector.azure.blob.core.api.BlobStoreApi;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createRequest;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createSharedKey;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureStorageDataSourceTest {

    private final String content = "test-content";
    private final Monitor monitor = mock(Monitor.class);
    private final BlobStoreApi blobStoreApi = mock(BlobStoreApi.class);
    private final DataFlowRequest.Builder request = createRequest(AzureBlobStoreSchema.TYPE);
    private final String accountName = createAccountName();
    private final String containerName = createContainerName();
    private final String sharedKey = createSharedKey();
    private final String blobName = createBlobName();
    private final Exception exception = new TestCustomException("test-message");

    AzureStorageDataSource dataSource = AzureStorageDataSource.Builder.newInstance()
            .accountName(accountName)
            .containerName(containerName)
            .blobName(blobName)
            .sharedKey(sharedKey)
            .requestId(request.build().getId())
            .retryPolicy(RetryPolicy.ofDefaults())
            .blobStoreApi(blobStoreApi)
            .monitor(monitor)
            .build();
    BlobAdapter destination = mock(BlobAdapter.class);
    ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(UTF_8));

    @BeforeEach
    void setUp() {
        when(destination.openInputStream()).thenReturn(input);
        when(blobStoreApi.getBlobAdapter(
                accountName,
                containerName,
                blobName,
                sharedKey))
                .thenReturn(destination);
    }

    @Test
    void openPartStream_succeeds() {
        var result = dataSource.openPartStream();
        assertThat(result).map(s -> s.openStream()).containsExactly(input);
    }

    @Test
    void openPartStream_whenBlobClientCreationFails_fails() {
        when(blobStoreApi.getBlobAdapter(
                accountName,
                containerName,
                blobName,
                sharedKey))
                .thenThrow(exception);

        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() -> dataSource.openPartStream())
                .withCause(exception);
        verify(monitor).severe(format("Error accessing blob %s on account %s", blobName, accountName), exception);
    }
}