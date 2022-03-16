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

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.AzureDataFactoryIntegrationTest;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.BLOB_NAME;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.SHARED_KEY;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.TYPE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cloud integration test that takes configuration from Terraform outputs and runs a data transfer
 * across blob storage accounts using Azure Data Factory.
 */
@AzureDataFactoryIntegrationTest
@ExtendWith(EdcExtension.class)
class AzureDataFactoryCopyIntegrationTest {

    private static List<Runnable> containerCleanup = new ArrayList<>();
    private static Properties savedProperties;
    private static final String RUNTIME_SETTINGS_PATH = "resources/azure/testing/runtime_settings.properties";
    private static final String EDC_FS_CONFIG = "edc.fs.config";
    private static final String PROVIDER_STORAGE_RESOURCE_ID = "test.provider.storage.resourceid";
    private static final String CONSUMER_STORAGE_RESOURCE_ID = "test.consumer.storage.resourceid";

    private final String blobName = createBlobName();

    @BeforeAll
    static void beforeAll() throws FileNotFoundException {
        savedProperties = (Properties) System.getProperties().clone();
        var file = new File(TestUtils.findBuildRoot(), RUNTIME_SETTINGS_PATH);
        if (!file.exists()) {
            throw new FileNotFoundException("Runtime settings file not found");
        }
        System.setProperty(EDC_FS_CONFIG, file.getAbsolutePath());
    }

    @AfterAll
    static void afterAll() {
        System.setProperties(savedProperties);
        containerCleanup.parallelStream().forEach(Runnable::run);
    }

    @Test
    void transfer_success(
            EdcExtension edc,
            AzureResourceManager azure,
            DataPlaneManager dataPlaneManager,
            DataPlaneStore store) {
        // Arrange
        var providerStorage = new Account(azure, edc, PROVIDER_STORAGE_RESOURCE_ID);
        var consumerStorage = new Account(azure, edc, CONSUMER_STORAGE_RESOURCE_ID);
        var randomBytes = new byte[1024];
        var random = new Random();
        random.nextBytes(randomBytes);

        providerStorage.client
                .getBlobContainerClient(providerStorage.containerName)
                .getBlobClient(blobName)
                .getBlockBlobClient()
                .upload(new ByteArrayInputStream(randomBytes), randomBytes.length);

        var source = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, providerStorage.name)
                .property(CONTAINER_NAME, providerStorage.containerName)
                .property(BLOB_NAME, blobName)
                .property(SHARED_KEY, providerStorage.key)
                .build();
        var destination = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, consumerStorage.name)
                .property(CONTAINER_NAME, consumerStorage.containerName)
                .property(SHARED_KEY, consumerStorage.key)
                .build();
        var request = DataFlowRequest.Builder.newInstance()
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .trackable(true)
                .build();

        // Act
        dataPlaneManager.initiateTransfer(request);

        // Assert
        var destinationBlob = consumerStorage.client
                .getBlobContainerClient(consumerStorage.containerName)
                .getBlobClient(blobName);
        await()
                .atMost(Duration.ofMinutes(5))
                .untilAsserted(() -> assertThat(store.getState(request.getProcessId()))
                        .isEqualTo(DataPlaneStore.State.COMPLETED));
        assertThat(destinationBlob.exists())
                .withFailMessage("should have copied blob between containers")
                .isTrue();
        assertThat(destinationBlob.getProperties().getBlobSize())
                .isEqualTo(randomBytes.length);
    }

    static class Account {

        static final Faker FAKER = new Faker();
        final String name;
        final String key;
        final BlobServiceClient client;
        final String containerName = FAKER.lorem().characters(35, 40, false, false);

        Account(AzureResourceManager azure, EdcExtension edc, String setting) {
            String accountId = Objects.requireNonNull(edc.getContext().getConfig().getString(setting), setting);
            var account = azure.storageAccounts().getById(accountId);
            name = account.name();
            key = account.getKeys().stream().findFirst().orElseThrow().value();
            client = new BlobServiceClientBuilder()
                    .credential(new StorageSharedKeyCredential(account.name(), key))
                    .endpoint(account.endPoints().primary().blob())
                    .buildClient();
            createContainer();
        }

        void createContainer() {
            assertFalse(client.getBlobContainerClient(containerName).exists());

            BlobContainerClient blobContainerClient = client.createBlobContainer(containerName);
            assertTrue(blobContainerClient.exists());
            containerCleanup.add(() -> client.deleteBlobContainer(containerName));
        }
    }
}
