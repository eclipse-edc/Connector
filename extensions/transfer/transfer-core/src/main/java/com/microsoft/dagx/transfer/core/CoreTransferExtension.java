package com.microsoft.dagx.transfer.core;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.transfer.core.flow.DataFlowManagerImpl;
import com.microsoft.dagx.transfer.core.provision.ProvisionManagerImpl;

/**
 * Provides core data transfer services to the system.
 */
public class CoreTransferExtension implements ServiceExtension {
    private Monitor monitor;
    private ProvisionManagerImpl provisionManager;
    private ServiceExtensionContext context;

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        this.context = context;
        registerTypes(context.getTypeManager());

        provisionManager = new ProvisionManagerImpl();
        context.registerService(ProvisionManager.class, provisionManager);
        context.registerService(DataFlowManager.class, new DataFlowManagerImpl());

        monitor.info("Initialized Core Transfer extension");
    }

    @Override
    public void start() {
        var transferProcessStore = context.getService(TransferProcessStore.class);
        provisionManager.start(transferProcessStore);
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
