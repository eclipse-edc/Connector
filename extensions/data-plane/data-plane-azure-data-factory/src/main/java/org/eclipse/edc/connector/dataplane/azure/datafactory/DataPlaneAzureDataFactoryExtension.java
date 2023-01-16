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

package org.eclipse.edc.connector.dataplane.azure.datafactory;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.datafactory.DataFactoryManager;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.SettingResolver;
import org.eclipse.edc.spi.types.TypeManager;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * Registers a {@link TransferService} for performing data transfers with Azure Data Factory.
 */
@Extension(value = DataPlaneAzureDataFactoryExtension.NAME)
public class DataPlaneAzureDataFactoryExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Azure Data Factory";
    @Setting
    private static final String KEY_VAULT_LINKED_SERVICE_NAME = "edc.data.factory.key.vault.linkedservicename";
    @Setting
    private static final String RESOURCE_ID = "edc.data.factory.resource.id";
    @Setting
    private static final String KEY_VAULT_RESOURCE_ID = "edc.data.factory.key.vault.resource.id";
    @Setting
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

    @Inject
    private Clock clock;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
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
                typeManager);
        var pollDelay = Duration.ofMillis(context.getSetting(DATA_FACTORY_POLL_DELAY, 5000L));
        var transferManager = new AzureDataFactoryTransferManager(
                monitor,
                dataFactoryClient,
                pipelineFactory,
                maxDuration,
                clock,
                blobStoreApi,
                typeManager,
                keyVaultClient,
                pollDelay);
        var transferService = new AzureDataFactoryTransferService(
                validator,
                transferManager);
        registry.registerTransferService(transferService);
    }

    private String requiredSetting(SettingResolver context, String s) {
        return Objects.requireNonNull(context.getSetting(s, null), s);
    }
}
