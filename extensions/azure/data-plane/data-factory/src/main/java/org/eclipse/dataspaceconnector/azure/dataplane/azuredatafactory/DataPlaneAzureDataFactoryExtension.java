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

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.datafactory.DataFactoryManager;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.eclipse.dataspaceconnector.azure.blob.core.api.BlobStoreApi;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.TransferService;
import org.eclipse.dataspaceconnector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.SettingResolver;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * Registers a {@link TransferService} for performing data transfers with Azure Data Factory.
 */
public class DataPlaneAzureDataFactoryExtension implements ServiceExtension {

    @EdcSetting
    private static final String KEY_VAULT_LINKED_SERVICE_NAME = "edc.data.factory.key.vault.linkedservicename";
    @EdcSetting
    private static final String RESOURCE_ID = "edc.data.factory.resource.id";
    @EdcSetting
    private static final String KEY_VAULT_RESOURCE_ID = "edc.data.factory.key.vault.resource.id";
    @EdcSetting
    private static final String DATA_FACTORY_POLL_DELAY = "edc.data.factory.poll.delay.ms";

    @Inject
    private TransferServiceRegistry registry;

    @Inject
    private AzureProfile profile;

    @Inject
    private AzureResourceManager resourceManager;

    @Inject
    private TokenCredential credential;
    @Inject
    private BlobStoreApi blobStoreApi;

    @Inject(required = false)
    private final Clock clock = Clock.systemUTC();

    @Override
    public String name() {
        return "Data Plane Azure Data Factory";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var dataFactoryId = requiredSetting(context, RESOURCE_ID);
        var keyVaultId = requiredSetting(context, KEY_VAULT_RESOURCE_ID);
        var keyVaultLinkedService = context.getSetting(KEY_VAULT_LINKED_SERVICE_NAME, "AzureKeyVault");

        var dataFactoryManager = DataFactoryManager.authenticate(credential, profile);
        var factory = resourceManager.genericResources().getById(dataFactoryId);
        var vault = resourceManager.vaults().getById(keyVaultId);

        var secretClient = new SecretClientBuilder()
                .vaultUrl(vault.vaultUri())
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        var maxDuration = Duration.ofHours(1);
        var dataFactoryClient = new DataFactoryClient(dataFactoryManager, factory.resourceGroupName(), factory.name());
        var keyVaultClient = new KeyVaultClient(secretClient);
        var validator = new AzureDataFactoryTransferRequestValidator();
        var pipelineFactory = new DataFactoryPipelineFactory(
                keyVaultLinkedService,
                keyVaultClient,
                dataFactoryClient,
                context.getTypeManager());
        var pollDelay = Duration.ofMillis(context.getSetting(DATA_FACTORY_POLL_DELAY, 5000L));
        var transferManager = new AzureDataFactoryTransferManager(
                monitor,
                dataFactoryClient,
                pipelineFactory,
                maxDuration,
                clock,
                blobStoreApi,
                context.getTypeManager(),
                keyVaultClient,
                pollDelay);
        var transferService = new AzureDataFactoryTransferService(
                validator,
                transferManager);
        registry.registerTransferService(transferService);
    }

    private static String requiredSetting(SettingResolver context, String s) {
        return Objects.requireNonNull(context.getSetting(s, null), s);
    }
}
