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

package org.eclipse.dataspaceconnector.system.tests.local;

import org.eclipse.dataspaceconnector.azure.blob.core.api.BlobStoreApiImpl;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.AzureDataFactoryIntegrationTest;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.core.security.azure.AzureVault;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static java.lang.String.valueOf;
import static java.lang.System.getenv;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.system.tests.local.BlobTransferUtils.createAsset;
import static org.eclipse.dataspaceconnector.system.tests.local.BlobTransferUtils.createContractDefinition;
import static org.eclipse.dataspaceconnector.system.tests.local.BlobTransferUtils.createPolicy;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_CONNECTOR_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_CONNECTOR_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_IDS_API;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_IDS_API_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.IDS_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_CONNECTOR_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_CONNECTOR_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_IDS_API;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_IDS_API_PORT;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_MANAGEMENT_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.PROVIDER_MANAGEMENT_PORT;
import static org.eclipse.dataspaceconnector.system.tests.utils.GatlingUtils.runGatling;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.PROVIDER_ASSET_FILE;

@AzureDataFactoryIntegrationTest
public class AzureDataFactoryTransferIntegrationTest {

    private static final List<Runnable> containerCleanup = new ArrayList<>();
    private static final String EDC_FS_CONFIG = "edc.fs.config";
    private static final String EDC_VAULT_NAME = "edc.vault.name";
    private static final String EDC_VAULT_CLIENT_ID = "edc.vault.clientid";
    private static final String EDC_VAULT_TENANT_ID = "edc.vault.tenantid";
    private static final String EDC_VAULT_CLIENT_SECRET = "edc.vault.clientsecret";
    private static final String PROVIDER_CONTAINER_NAME = UUID.randomUUID().toString();
    private static final String KEY_VAULT_NAME = runtimeSettingsProperties().getProperty("test.key.vault.name");
    private static final String AZURE_TENANT_ID = getenv("AZURE_TENANT_ID");
    private static final String AZURE_CLIENT_ID = getenv("AZURE_CLIENT_ID");
    private static final String AZURE_CLIENT_SECRET = getenv("AZURE_CLIENT_SECRET");
    private static final String PROVIDER_STORAGE_ACCOUNT_NAME = runtimeSettingsProperties().getProperty("test.provider.storage.name");
    private static final String CONSUMER_STORAGE_ACCOUNT_NAME = runtimeSettingsProperties().getProperty("test.consumer.storage.name");
    private static final String BLOB_STORE_ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net";

    @RegisterExtension
    private static final EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-consumer",
            "consumer",
            Map.ofEntries(
                    Map.entry("web.http.port", valueOf(CONSUMER_CONNECTOR_PORT)),
                    Map.entry("web.http.path", CONSUMER_CONNECTOR_PATH),
                    Map.entry("web.http.data.port", valueOf(CONSUMER_MANAGEMENT_PORT)),
                    Map.entry("web.http.data.path", CONSUMER_MANAGEMENT_PATH),
                    Map.entry("web.http.ids.port", valueOf(CONSUMER_IDS_API_PORT)),
                    Map.entry("web.http.ids.path", IDS_PATH),
                    Map.entry("ids.webhook.address", CONSUMER_IDS_API),
                    Map.entry(EDC_VAULT_NAME, KEY_VAULT_NAME),
                    Map.entry(EDC_VAULT_CLIENT_ID, AZURE_CLIENT_ID),
                    Map.entry(EDC_VAULT_TENANT_ID, AZURE_TENANT_ID),
                    Map.entry(EDC_VAULT_CLIENT_SECRET, AZURE_CLIENT_SECRET)
            )
    );

    @RegisterExtension
    private static final EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-data-factory-transfer-provider",
            "provider",
            Map.ofEntries(
                    Map.entry("web.http.port", valueOf(PROVIDER_CONNECTOR_PORT)),
                    Map.entry("web.http.path", PROVIDER_CONNECTOR_PATH),
                    Map.entry("web.http.data.port", valueOf(PROVIDER_MANAGEMENT_PORT)),
                    Map.entry("web.http.data.path", PROVIDER_MANAGEMENT_PATH),
                    Map.entry("web.http.ids.port", valueOf(PROVIDER_IDS_API_PORT)),
                    Map.entry("web.http.ids.path", IDS_PATH),
                    Map.entry("ids.webhook.address", PROVIDER_IDS_API),
                    Map.entry(EDC_FS_CONFIG, runtimeSettingsPath()),
                    Map.entry(EDC_VAULT_NAME, KEY_VAULT_NAME),
                    Map.entry(EDC_VAULT_CLIENT_ID, AZURE_CLIENT_ID),
                    Map.entry(EDC_VAULT_TENANT_ID, AZURE_TENANT_ID),
                    Map.entry(EDC_VAULT_CLIENT_SECRET, AZURE_CLIENT_SECRET)
            )
    );

    @BeforeAll
    static void beforeAll() throws FileNotFoundException {
        var file = new File(runtimeSettingsPath());
        if (!file.exists()) {
            throw new FileNotFoundException("Runtime settings file not found");
        }
    }

    @AfterAll
    static void cleanUp() {
        containerCleanup.parallelStream().forEach(Runnable::run);
    }

    @Test
    public void transferBlob_success() {
        // Arrange
        var vault = AzureVault.authenticateWithSecret(new ConsoleMonitor(), AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET, KEY_VAULT_NAME);
        var blobStoreApi = new BlobStoreApiImpl(vault, BLOB_STORE_ENDPOINT_TEMPLATE);

        // Upload a blob with test data on provider blob container
        var blobContent = "AzureDataFactoryTransferIntegrationTest-" + UUID.randomUUID();

        blobStoreApi.createContainer(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME);
        blobStoreApi.putBlob(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME, PROVIDER_ASSET_FILE, blobContent.getBytes(UTF_8));
        // Add for cleanup
        containerCleanup.add(() -> blobStoreApi.deleteContainer(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME));

        // Seed data to provider
        createAsset(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME);
        var policyId = createPolicy();
        createContractDefinition(policyId);

        // Act
        System.setProperty(BlobTransferLocalSimulation.ACCOUNT_NAME_PROPERTY, CONSUMER_STORAGE_ACCOUNT_NAME);
        System.setProperty(BlobTransferLocalSimulation.MAX_DURATION_SECONDS_PROPERTY, "360"); // ADF SLA is to initiate copy within 4 minutes
        runGatling(BlobTransferLocalSimulation.class, TransferSimulationUtils.DESCRIPTION);

        // Assert
        var provisionedContainerName = BlobTransferUtils.getProvisionedContainerName();
        // Add for cleanup
        containerCleanup.add(() -> blobStoreApi.deleteContainer(CONSUMER_STORAGE_ACCOUNT_NAME, provisionedContainerName));

        var actualBlobContent = blobStoreApi.getBlob(CONSUMER_STORAGE_ACCOUNT_NAME, provisionedContainerName, PROVIDER_ASSET_FILE);
        assertThat(actualBlobContent.length)
                .withFailMessage("Destination blob %s not created", PROVIDER_ASSET_FILE)
                .isGreaterThan(0);
        assertThat(new String(actualBlobContent))
                .withFailMessage("Transferred file contents are not same as the source file")
                .isEqualTo(blobContent);

    }

    @NotNull
    private static String runtimeSettingsPath() {
        return new File(TestUtils.findBuildRoot(), "resources/azure/testing/runtime_settings.properties").getAbsolutePath();
    }

    @NotNull
    private static Properties runtimeSettingsProperties() {
        try (InputStream input = new FileInputStream(runtimeSettingsPath())) {
            Properties prop = new Properties();
            prop.load(input);

            return prop;
        } catch (IOException e) {
            throw new RuntimeException("Error in loading runtime settings properties", e);
        }
    }
}
