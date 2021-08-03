/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.transfer.core;

import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.transfer.TransferProcessManager;
import org.eclipse.edc.spi.transfer.TransferProcessObservable;
import org.eclipse.edc.spi.transfer.TransferWaitStrategy;
import org.eclipse.edc.spi.transfer.flow.DataFlowManager;
import org.eclipse.edc.spi.transfer.provision.ProvisionManager;
import org.eclipse.edc.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.edc.spi.transfer.store.TransferProcessStore;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataRequest;
import org.eclipse.edc.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.edc.transfer.core.flow.DataFlowManagerImpl;
import org.eclipse.edc.transfer.core.protocol.provider.RemoteMessageDispatcherRegistryImpl;
import org.eclipse.edc.transfer.core.provision.ProvisionManagerImpl;
import org.eclipse.edc.transfer.core.provision.ResourceManifestGeneratorImpl;
import org.eclipse.edc.transfer.core.transfer.ExponentialWaitStrategy;
import org.eclipse.edc.transfer.core.transfer.TransferProcessManagerImpl;

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
        return Set.of("edc:statuschecker", "edc:dispatcher", "edc:manifestgenerator", "edc:transfer-process-manager", "edc:transfer-process-observable");
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
