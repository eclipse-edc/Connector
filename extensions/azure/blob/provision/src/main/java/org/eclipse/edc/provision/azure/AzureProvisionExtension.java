/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.provision.azure;

import org.eclipse.edc.common.azure.BlobStoreApi;
import org.eclipse.edc.common.azure.BlobStoreApiImpl;
import org.eclipse.edc.provision.azure.blob.ObjectContainerProvisionedResource;
import org.eclipse.edc.provision.azure.blob.ObjectStorageDefinitionClientGenerator;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.transfer.provision.ProvisionManager;
import org.eclipse.edc.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.edc.provision.azure.blob.*;
import net.jodah.failsafe.RetryPolicy;

import java.util.Set;

/**
 * Provides data transfer {@link org.eclipse.edc.spi.transfer.provision.Provisioner}s backed by Azure services.
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
        return Set.of("edc:retry-policy");
    }

    @Override
    public Set<String> provides() {
        return Set.of("edc:blobstoreapi");
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
