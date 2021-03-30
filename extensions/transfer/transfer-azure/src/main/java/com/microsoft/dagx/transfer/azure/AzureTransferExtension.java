package com.microsoft.dagx.transfer.azure;

import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;

/**
 * Provides data transfer implementations backed by Azure services.
 */
public class AzureTransferExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        var provisionManager = context.getService(ProvisionManager.class);
        provisionManager.register(new ObjectStorageProvisioner());
    }
}
