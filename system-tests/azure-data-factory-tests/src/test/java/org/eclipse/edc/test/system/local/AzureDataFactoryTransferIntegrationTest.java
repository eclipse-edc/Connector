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

package org.eclipse.edc.test.system.local;

import org.eclipse.edc.azure.blob.api.BlobStoreApiImpl;
import org.eclipse.edc.azure.testfixtures.TestFunctions;
import org.eclipse.edc.azure.testfixtures.annotations.AzureDataFactoryIntegrationTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.test.system.utils.TransferTestRunner;
import org.eclipse.edc.vault.azure.AzureVault;
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
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.getenv;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.edc.test.system.local.BlobTransferConfiguration.BLOB_CONTENT;
import static org.eclipse.edc.test.system.local.BlobTransferUtils.createAsset;
import static org.eclipse.edc.test.system.local.BlobTransferUtils.createContractDefinition;
import static org.eclipse.edc.test.system.local.BlobTransferUtils.createPolicy;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_CONNECTOR_MANAGEMENT_URL;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_CONNECTOR_PATH;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_CONNECTOR_PORT;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_MANAGEMENT_PATH;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_MANAGEMENT_PORT;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_PROTOCOL_PORT;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_PROTOCOL_URL;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROTOCOL_PATH;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_ASSET_FILE;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_CONNECTOR_PATH;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_CONNECTOR_PORT;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_MANAGEMENT_PATH;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_MANAGEMENT_PORT;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_PROTOCOL_PORT;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_PROTOCOL_URL;

@AzureDataFactoryIntegrationTest
class AzureDataFactoryTransferIntegrationTest {

    private static final List<Runnable> CONTAINER_CLEANUP = new ArrayList<>();
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
    private static final EdcRuntimeExtension CONSUMER = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-consumer",
            "consumer",
            Map.ofEntries(
                    Map.entry("web.http.port", valueOf(CONSUMER_CONNECTOR_PORT)),
                    Map.entry("web.http.path", CONSUMER_CONNECTOR_PATH),
                    Map.entry("web.http.management.port", valueOf(CONSUMER_MANAGEMENT_PORT)),
                    Map.entry("web.http.management.path", CONSUMER_MANAGEMENT_PATH),
                    Map.entry("web.http.protocol.port", valueOf(CONSUMER_PROTOCOL_PORT)),
                    Map.entry("web.http.protocol.path", PROTOCOL_PATH),
                    Map.entry("edc.dsp.callback.address", CONSUMER_PROTOCOL_URL),
                    Map.entry(EDC_FS_CONFIG, runtimeSettingsPath()),
                    Map.entry(EDC_VAULT_NAME, KEY_VAULT_NAME),
                    Map.entry(EDC_VAULT_CLIENT_ID, AZURE_CLIENT_ID),
                    Map.entry(EDC_VAULT_TENANT_ID, AZURE_TENANT_ID),
                    Map.entry(EDC_VAULT_CLIENT_SECRET, AZURE_CLIENT_SECRET)
            )
    );

    @RegisterExtension
    private static final EdcRuntimeExtension PROVIDER = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-data-factory-transfer-provider",
            "provider",
            Map.ofEntries(
                    Map.entry("web.http.port", valueOf(PROVIDER_CONNECTOR_PORT)),
                    Map.entry("web.http.path", PROVIDER_CONNECTOR_PATH),
                    Map.entry("web.http.management.port", valueOf(PROVIDER_MANAGEMENT_PORT)),
                    Map.entry("web.http.management.path", PROVIDER_MANAGEMENT_PATH),
                    Map.entry("web.http.protocol.port", valueOf(PROVIDER_PROTOCOL_PORT)),
                    Map.entry("web.http.protocol.path", PROTOCOL_PATH),
                    Map.entry("edc.dsp.callback.address", PROVIDER_PROTOCOL_URL),
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
        CONTAINER_CLEANUP.parallelStream().forEach(Runnable::run);
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

    @Test
    void transferBlob_success() {
        // Arrange
        var vault = AzureVault.authenticateWithSecret(new ConsoleMonitor(), AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET, KEY_VAULT_NAME);
        var account2Key = Objects.requireNonNull(vault.resolveSecret(format("%s-key1", CONSUMER_STORAGE_ACCOUNT_NAME)));
        var blobStoreApi = new BlobStoreApiImpl(vault, BLOB_STORE_ENDPOINT_TEMPLATE);

        // Upload a blob with test data on provider blob container
        blobStoreApi.createContainer(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME);
        blobStoreApi.putBlob(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME, PROVIDER_ASSET_FILE, BLOB_CONTENT.getBytes(UTF_8));
        // Add for cleanup
        CONTAINER_CLEANUP.add(() -> blobStoreApi.deleteContainer(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME));

        // Seed data to provider
        createAsset(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME);
        var policyId = createPolicy();
        createContractDefinition(policyId);


        var blobServiceClient = TestFunctions.getBlobServiceClient(CONSUMER_STORAGE_ACCOUNT_NAME, account2Key, TestFunctions.getBlobServiceTestEndpoint(format("https://%s.blob.core.windows.net", CONSUMER_STORAGE_ACCOUNT_NAME)));

        var runner = new TransferTestRunner(new BlobTransferConfiguration(CONSUMER_CONNECTOR_MANAGEMENT_URL, PROVIDER_PROTOCOL_URL, blobServiceClient, 360));

        runner.executeTransfer();
    }
}
