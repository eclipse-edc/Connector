package com.microsoft.dagx.ids.transfer.core;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.TransferManagerRegistry;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;

/**
 * Provides core data transfer services to the system.
 */
public class CoreTransferExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerTypes(context.getTypeManager());

        context.registerService(TransferManagerRegistry.class, new TransferManagerRegistryImpl());

        monitor.info("Initialized Core Transfer extension");
    }

    @Override
    public void start() {
        monitor.info("Started Core Transfer extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Core Transfer extension");
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(DataRequest.class);
    }

}
