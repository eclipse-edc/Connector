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
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureSasToken;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.AzureDataFactoryIntegrationTest;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.BLOB_NAME;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.TYPE;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createBlobName;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cloud integration test that takes configuration from Terraform outputs and runs a data transfer across blob storage
 * accounts using Azure Data Factory.
 */
@AzureDataFactoryIntegrationTest
@ExtendWith(EdcExtension.class)
class AzureDataFactoryCopyIntegrationTest {

    private static final String RUNTIME_SETTINGS_PATH = "resources/azure/testing/runtime_settings.properties";
    private static final String EDC_FS_CONFIG = "edc.fs.config";
    private static final String PROVIDER_STORAGE_RESOURCE_ID = "test.provider.storage.resourceid";
    private static final String CONSUMER_STORAGE_RESOURCE_ID = "test.consumer.storage.resourceid";
    private static final String KEY_VAULT_RESOURCE_ID = "edc.data.factory.key.vault.resource.id";
    private static final List<Runnable> CONTAINER_CLEANUP = new ArrayList<>();
    private static final List<Runnable> SECRET_CLEANUP = new ArrayList<>();
    private static Properties savedProperties;
    private final String blobName = createBlobName();
    private final TypeManager typeManager = new TypeManager();

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
        CONTAINER_CLEANUP.parallelStream().forEach(Runnable::run);
        SECRET_CLEANUP.forEach(Runnable::run);
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
                .keyName(providerStorage.name + "-key1")
                .build();

        var destSecretKeyName = consumerStorage.name + "-ittest-sas-" + UUID.randomUUID();
        var destination = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, consumerStorage.name)
                .property(CONTAINER_NAME, consumerStorage.containerName)
                .keyName(destSecretKeyName)
                .build();

        var request = DataFlowRequest.Builder.newInstance()
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .trackable(true)
                .build();

        // Generate write-only sas for destination container and store as secret
        var vault = azure.vaults()
                .getById(Objects.requireNonNull(edc.getContext().getConfig().getString(KEY_VAULT_RESOURCE_ID), KEY_VAULT_RESOURCE_ID));
        setSecret(consumerStorage, vault, destSecretKeyName);

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

    private void setSecret(Account account, Vault vault, String secretName) {
        // ADF SLA to start an activity is 4 minutes.
        var expiryTime = OffsetDateTime.now().plusMinutes(8);
        var permission = new BlobContainerSasPermission().setWritePermission(true);
        var sasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, permission)
                .setStartTime(OffsetDateTime.now());
        var sasToken = account.client
                .getBlobContainerClient(account.containerName)
                .generateSas(sasSignatureValues);
        var edcAzureSas = new AzureSasToken(sasToken, expiryTime.toEpochSecond());
        // Set Secret
        vault.secretClient().setSecret(secretName, typeManager.writeValueAsString(edcAzureSas)).block(Duration.ofMinutes(1));
        // Add for clean up test data
        SECRET_CLEANUP.add(() -> vault.secretClient().beginDeleteSecret(secretName).blockLast(Duration.ofMinutes(1)));
        SECRET_CLEANUP.add(() -> vault.secretClient().purgeDeletedSecret(secretName).block(Duration.ofMinutes(1)));
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
            CONTAINER_CLEANUP.add(() -> client.deleteBlobContainer(containerName));
        }
    }
}
