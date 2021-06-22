/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.core;

import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.transfer.TransferProcessObservable;
import com.microsoft.dagx.spi.transfer.TransferWaitStrategy;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.StatusCheckerRegistry;
import com.microsoft.dagx.transfer.core.flow.DataFlowManagerImpl;
import com.microsoft.dagx.transfer.core.protocol.provider.RemoteMessageDispatcherRegistryImpl;
import com.microsoft.dagx.transfer.core.provision.ProvisionManagerImpl;
import com.microsoft.dagx.transfer.core.provision.ResourceManifestGeneratorImpl;
import com.microsoft.dagx.transfer.core.transfer.ExponentialWaitStrategy;
import com.microsoft.dagx.transfer.core.transfer.TransferProcessManagerImpl;

import java.util.Set;

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
        monitor = context.getMonitor();
        this.context = context;

        var typeManager = context.getTypeManager();

        registerTypes(typeManager);

        var dataFlowManager = new DataFlowManagerImpl();
        context.registerService(DataFlowManager.class, dataFlowManager);

        var dispatcherRegistry = new RemoteMessageDispatcherRegistryImpl();
        context.registerService(RemoteMessageDispatcherRegistry.class, dispatcherRegistry);

        var manifestGenerator = new ResourceManifestGeneratorImpl();
        context.registerService(ResourceManifestGenerator.class, manifestGenerator);

        var statusCheckerRegistry = new StatusCheckerRegistryImpl();
        context.registerService(StatusCheckerRegistry.class, statusCheckerRegistry);

        var vault = context.getService(Vault.class);

        provisionManager = new ProvisionManagerImpl(vault, typeManager, monitor);
        context.registerService(ProvisionManager.class, provisionManager);

        var waitStrategy = context.hasService(TransferWaitStrategy.class) ? context.getService(TransferWaitStrategy.class) : new ExponentialWaitStrategy(DEFAULT_ITERATION_WAIT);

        processManager = TransferProcessManagerImpl.Builder.newInstance()
                .waitStrategy(waitStrategy)
                .manifestGenerator(manifestGenerator)
                .dataFlowManager(dataFlowManager)
                .provisionManager(provisionManager)
                .dispatcherRegistry(dispatcherRegistry)
                .statusCheckerRegistry(statusCheckerRegistry)
                .monitor(monitor)
                .build();

        context.registerService(TransferProcessManager.class, processManager);
        context.registerService(TransferProcessObservable.class, processManager);

        monitor.info("Initialized Core Transfer extension");
    }

    @Override
    public Set<String> provides() {
        return Set.of("dagx:statuschecker", "dagx:dispatcher", "dagx:manifestgenerator");
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
