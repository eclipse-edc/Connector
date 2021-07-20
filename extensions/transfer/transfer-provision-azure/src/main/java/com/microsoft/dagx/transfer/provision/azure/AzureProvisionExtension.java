/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.provision.azure;

import com.microsoft.dagx.common.azure.BlobStoreApi;
import com.microsoft.dagx.common.azure.BlobStoreApiImpl;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.transfer.StatusCheckerRegistry;
import com.microsoft.dagx.transfer.provision.azure.blob.*;
import net.jodah.failsafe.RetryPolicy;

import java.util.Set;

/**
 * Provides data transfer {@link com.microsoft.dagx.spi.transfer.provision.Provisioner}s backed by Azure services.
 */
public class AzureProvisionExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {

        monitor = context.getMonitor();
        var provisionManager = context.getService(ProvisionManager.class);

        context.registerService(BlobStoreApi.class, new BlobStoreApiImpl(context.getService(Vault.class)));

        //noinspection unchecked
        var retryPolicy = (RetryPolicy<Object>) context.getService(RetryPolicy.class);
        final BlobStoreApi blobStoreApi = context.getService(BlobStoreApi.class);
        provisionManager.register(new ObjectStorageProvisioner(retryPolicy, monitor, blobStoreApi));

        // register the generator
        var manifestGenerator = context.getService(ResourceManifestGenerator.class);
        manifestGenerator.registerClientGenerator(new ObjectStorageDefinitionClientGenerator());

        var statusCheckerReg = context.getService(StatusCheckerRegistry.class);
        statusCheckerReg.register(ObjectContainerProvisionedResource.class, new ObjectContainerStatusChecker(blobStoreApi, retryPolicy));

        registerTypes(context.getTypeManager());

        monitor.info("Initialized Azure Provision extension");
    }

    @Override
    public Set<String> requires() {
        return Set.of("dagx:retry-policy");
    }

    @Override
    public Set<String> provides() {
        return Set.of("dagx:blobstoreapi");
    }

    @Override
    public void start() {
        monitor.info("Started Azure Provision extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Azure Provision extension");
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(ObjectContainerProvisionedResource.class, ObjectStorageResourceDefinition.class, AzureSasToken.class);
    }

}
