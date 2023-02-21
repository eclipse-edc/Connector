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

package org.eclipse.edc.connector.dataplane.azure.storage.pipeline;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AzureStorageDataSinkFactoryTest {
    private final BlobStoreApi blobStoreApi = mock(BlobStoreApi.class);
    private final Vault vault = mock(Vault.class);
    private final TypeManager typeManager = new TypeManager();
    private final AzureStorageDataSinkFactory factory = new AzureStorageDataSinkFactory(blobStoreApi, Executors.newFixedThreadPool(1), 5, mock(Monitor.class), vault, typeManager);
    private final DataFlowRequest.Builder request = createRequest(AzureBlobStoreSchema.TYPE);
    private final DataFlowRequest.Builder invalidRequest = createRequest("test-type");
    private final DataAddress.Builder dataAddress = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE);

    private final String accountName = createAccountName();
    private final String containerName = createContainerName();
    private final String keyName = "test-keyname";
    private final AzureSasToken token = new AzureSasToken("test-writeonly-sas", new Random().nextLong());

    @Test
    void canHandle_whenBlobRequest_returnsTrue() {
        assertThat(factory.canHandle(request.build())).isTrue();
    }

    @Test
    void canHandle_whenNotBlobRequest_returnsFalse() {
        assertThat(factory.canHandle(invalidRequest.build())).isFalse();
    }

    @Test
    void validate_whenRequestValid_succeeds() {
        assertThat(factory.validateRequest(request.destinationDataAddress(dataAddress
                                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                                .keyName(keyName)
                                .build())
                        .build())
                .succeeded()).isTrue();
    }

    @Test
    void validate_whenMissingAccountName_fails() {
        assertThat(factory.validateRequest(request.destinationDataAddress(dataAddress
                                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                                .keyName(keyName)
                                .build())
                        .build())
                .failed()).isTrue();
    }

    @Test
    void validate_whenMissingContainerName_fails() {
        assertThat(factory.validateRequest(request.destinationDataAddress(dataAddress
                                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                                .keyName(keyName)
                                .build())
                        .build())
                .failed()).isTrue();
    }

    @Test
    void validate_whenMissingKeyName_fails() {
        assertThat(factory.validateRequest(request.destinationDataAddress(dataAddress
                                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                                .build())
                        .build())
                .failed()).isTrue();
    }

    @Test
    void createSink_whenValidRequest_succeeds() {
        when(vault.resolveSecret(keyName)).thenReturn(typeManager.writeValueAsString(token));
        var validRequest = request.destinationDataAddress(dataAddress
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                .keyName(keyName)
                .build());
        assertThat(factory.createSink(validRequest.build())).isNotNull();
    }

    @Test
    void createSink_whenInvalidRequest_fails() {
        assertThrows(EdcException.class, () -> factory.createSink(invalidRequest.build()));
    }
}
