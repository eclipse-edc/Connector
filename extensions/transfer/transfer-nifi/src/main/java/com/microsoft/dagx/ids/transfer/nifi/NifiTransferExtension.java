package com.microsoft.dagx.ids.transfer.nifi;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.TransferManagerRegistry;

public class NifiTransferExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        TransferManagerRegistry transferManagerRegistry = context.getService(TransferManagerRegistry.class);
        transferManagerRegistry.register(new NifiTransferManager());

        monitor.info("Initialized Core Transfer extension");
    }

    @Override
    public void start() {
        monitor.info("Started Nifi Transfer extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Nifi Transfer extension");
    }
}
