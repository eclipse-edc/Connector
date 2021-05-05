/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.provision.azure;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.types.TypeManager;

/**
 * Provides data transfer {@link com.microsoft.dagx.spi.transfer.provision.Provisioner}s backed by Azure services.
 */
public class AzureProvisionExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerTypes(context.getTypeManager());

        var provisionManager = context.getService(ProvisionManager.class);
        provisionManager.register(new ObjectStorageProvisioner());
        monitor = context.getMonitor();
        monitor.info("Initialized Azure Provision extension");
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
        typeManager.registerTypes(ObjectContainerProvisionedResource.class);
    }

}
