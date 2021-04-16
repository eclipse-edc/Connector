package com.microsoft.dagx.transfer.core;

import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.transfer.core.flow.DataFlowManagerImpl;
import com.microsoft.dagx.transfer.core.protocol.provider.RemoteMessageDispatcherRegistryImpl;
import com.microsoft.dagx.transfer.core.provision.ProvisionManagerImpl;
import com.microsoft.dagx.transfer.core.provision.ResourceManifestGeneratorImpl;
import com.microsoft.dagx.transfer.core.transfer.ExponentialWaitStrategy;
import com.microsoft.dagx.transfer.core.transfer.TransferProcessManagerImpl;

/**
 * Provides core data transfer services to the system.
 */
public class CoreTransferExtension implements ServiceExtension {
    private static final long DEFAULT_ITERATION_WAIT = 5000; // millis

    private Monitor monitor;
    private ServiceExtensionContext context;

    private ProvisionManagerImpl provisionManager;
    private TransferProcessManagerImpl processManager;

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.monitor = context.getMonitor();
        this.context = context;

        registerTypes(context.getTypeManager());

        var dataFlowManager = new DataFlowManagerImpl();
        context.registerService(DataFlowManager.class, dataFlowManager);

        var dispatcherRegistry = new RemoteMessageDispatcherRegistryImpl();
        context.registerService(RemoteMessageDispatcherRegistry.class, dispatcherRegistry);

        var manifestGenerator = new ResourceManifestGeneratorImpl();
        context.registerService(ResourceManifestGenerator.class, manifestGenerator);

        provisionManager = new ProvisionManagerImpl(context.getService(Vault.class));
        context.registerService(ProvisionManager.class, provisionManager);

        processManager = TransferProcessManagerImpl.Builder.newInstance()
                .waitStrategy(new ExponentialWaitStrategy(DEFAULT_ITERATION_WAIT))  // TODO make configurable
                .manifestGenerator(manifestGenerator)
                .dataFlowManager(dataFlowManager)
                .provisionManager(provisionManager)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .build();

        context.registerService(TransferProcessManager.class, processManager);

        monitor.info("Initialized Core Transfer extension");
    }

    @Override
    public void start() {
        var transferProcessStore = context.getService(TransferProcessStore.class);

        provisionManager.start(transferProcessStore);
        processManager.start(transferProcessStore);

        monitor.info("Started Core Transfer extension");
    }

    @Override
    public void shutdown() {
        if (processManager != null) {
            processManager.stop();
        }
        monitor.info("Shutdown Core Transfer extension");
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(DataRequest.class);
    }

}
