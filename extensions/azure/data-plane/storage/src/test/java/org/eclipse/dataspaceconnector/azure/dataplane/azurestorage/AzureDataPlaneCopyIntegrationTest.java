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
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.blob.core.api.BlobStoreApiImpl;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageDataSinkFactory;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageDataSourceFactory;
import org.eclipse.dataspaceconnector.azure.testfixtures.AbstractAzureBlobTest;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.AzureStorageIntegrationTest;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.BLOB_NAME;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.SHARED_KEY;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.TYPE;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createContainerName;
import static org.mockito.Mockito.mock;

@AzureStorageIntegrationTest
class AzureDataPlaneCopyIntegrationTest extends AbstractAzureBlobTest {

    static Faker faker = new Faker();

    RetryPolicy<Object> policy = new RetryPolicy<>().withMaxRetries(1);
    String sinkContainerName = createContainerName();
    String blobName = createBlobName();
    String content = faker.lorem().sentence();
    ExecutorService executor = Executors.newFixedThreadPool(2);
    Monitor monitor = mock(Monitor.class);

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
                .property(SHARED_KEY, account1Key)
                .build();
        var destination = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, account2Name)
                .property(CONTAINER_NAME, sinkContainerName)
                .property(SHARED_KEY, account2Key)
                .build();
        var request = DataFlowRequest.Builder.newInstance()
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .build();

        var dataSource = new AzureStorageDataSourceFactory(new BlobStoreApiImpl(null, getEndpoint(account1Name)), policy, monitor)
                .createSource(request);

        int partitionSize = 5;
        var dataSink = new AzureStorageDataSinkFactory(new BlobStoreApiImpl(null, getEndpoint(account2Name)), executor, partitionSize, monitor)
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
