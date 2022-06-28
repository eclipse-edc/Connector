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

package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage;

import com.azure.core.util.BinaryData;
import com.github.javafaker.Faker;
import dev.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureSasToken;
import org.eclipse.dataspaceconnector.azure.blob.core.api.BlobStoreApi;
import org.eclipse.dataspaceconnector.azure.blob.core.api.BlobStoreApiImpl;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageDataSinkFactory;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageDataSourceFactory;
import org.eclipse.dataspaceconnector.azure.testfixtures.AbstractAzureBlobTest;
import org.eclipse.dataspaceconnector.azure.testfixtures.TestFunctions;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.AzureStorageIntegrationTest;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.BLOB_NAME;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.TYPE;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createContainerName;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@AzureStorageIntegrationTest
class AzureDataPlaneCopyIntegrationTest extends AbstractAzureBlobTest {

    static Faker faker = new Faker();
    private final TypeManager typeManager = new TypeManager();

    String account1KeyName = faker.lorem().word() + "1";
    String account2KeyName = faker.lorem().word() + "2";
    RetryPolicy<Object> policy = RetryPolicy.builder().withMaxRetries(1).build();
    String sinkContainerName = createContainerName();
    String blobName = createBlobName();
    String content = faker.lorem().sentence();
    ExecutorService executor = Executors.newFixedThreadPool(2);
    Monitor monitor = mock(Monitor.class);
    Vault vault = mock(Vault.class);

    BlobStoreApi account1Api = new BlobStoreApiImpl(vault, TestFunctions.getBlobServiceTestEndpoint(account1Name));
    BlobStoreApi account2Api = new BlobStoreApiImpl(vault, TestFunctions.getBlobServiceTestEndpoint(account2Name));

    @BeforeEach
    void setUp() {
        createContainer(blobServiceClient2, sinkContainerName);
    }

    @Test
    void transfer_success() {
        blobServiceClient1.getBlobContainerClient(account1ContainerName)
                .getBlobClient(blobName)
                .upload(BinaryData.fromString(content));

        var source = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, account1Name)
                .property(CONTAINER_NAME, account1ContainerName)
                .property(BLOB_NAME, blobName)
                .keyName(account1KeyName)
                .build();
        when(vault.resolveSecret(account1KeyName)).thenReturn(account1Key);

        var destination = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, account2Name)
                .property(CONTAINER_NAME, sinkContainerName)
                .keyName(account2KeyName)
                .build();

        when(vault.resolveSecret(account2Name + "-key1"))
                .thenReturn(account2Key);
        var account2SasToken = account2Api.createContainerSasToken(account2Name, sinkContainerName, "w", OffsetDateTime.MAX.minusDays(1));
        var secretToken = new AzureSasToken(account2SasToken, Long.MAX_VALUE);
        when(vault.resolveSecret(account2KeyName))
                .thenReturn(typeManager.writeValueAsString(secretToken));

        var request = DataFlowRequest.Builder.newInstance()
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .build();

        var dataSource = new AzureStorageDataSourceFactory(account1Api, policy, monitor, vault)
                .createSource(request);

        int partitionSize = 5;
        var dataSink = new AzureStorageDataSinkFactory(account2Api, executor, partitionSize, monitor, vault, new TypeManager())
                .createSink(request);

        assertThat(dataSink.transfer(dataSource))
                .succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.succeeded()).isTrue());

        var destinationBlob = blobServiceClient2
                .getBlobContainerClient(sinkContainerName)
                .getBlobClient(blobName);
        assertThat(destinationBlob.exists())
                .withFailMessage("should have copied blob between containers")
                .isTrue();
        assertThat(destinationBlob.downloadContent())
                .asString()
                .isEqualTo(content);
    }
}
