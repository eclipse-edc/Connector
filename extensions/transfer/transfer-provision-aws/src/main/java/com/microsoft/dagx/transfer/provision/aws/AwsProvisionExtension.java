package com.microsoft.dagx.transfer.provision.aws;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;

/**
 * Provides data transfer {@link com.microsoft.dagx.spi.transfer.provision.Provisioner}s backed by Azure services.
 */
public class AwsProvisionExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var provisionManager = context.getService(ProvisionManager.class);

        monitor = context.getMonitor();
        monitor.info("Initialized AWS Provision extension");
    }

    @Override
    public void start() {
        monitor.info("Started AWS Provision extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown AWS Provision extension");
    }

}


