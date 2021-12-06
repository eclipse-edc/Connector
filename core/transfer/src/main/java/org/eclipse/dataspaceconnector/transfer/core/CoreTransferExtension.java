/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core;

import org.eclipse.dataspaceconnector.core.base.ExponentialWaitStrategy;
import org.eclipse.dataspaceconnector.core.base.RemoteMessageDispatcherRegistryImpl;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessObservable;
import org.eclipse.dataspaceconnector.spi.transfer.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.DataProxyManager;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntryHandlerRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.transfer.core.flow.DataFlowManagerImpl;
import org.eclipse.dataspaceconnector.transfer.core.provision.ProvisionManagerImpl;
import org.eclipse.dataspaceconnector.transfer.core.provision.ResourceManifestGeneratorImpl;
import org.eclipse.dataspaceconnector.transfer.core.synchronous.DataProxyManagerImpl;
import org.eclipse.dataspaceconnector.transfer.core.transfer.AsyncTransferProcessManager;
import org.eclipse.dataspaceconnector.transfer.core.transfer.DefaultProxyEntryHandlerRegistry;
import org.eclipse.dataspaceconnector.transfer.core.transfer.DelegatingTransferProcessManager;
import org.eclipse.dataspaceconnector.transfer.core.transfer.ExponentialWaitStrategy;
import org.eclipse.dataspaceconnector.transfer.core.transfer.StatusCheckerRegistryImpl;
import org.eclipse.dataspaceconnector.transfer.core.transfer.SyncTransferProcessManager;

import java.util.Set;

/**
 * Provides core data transfer services to the system.
 */
public class CoreTransferExtension implements ServiceExtension {
    private static final long DEFAULT_ITERATION_WAIT = 5000; // millis

    private ServiceExtensionContext context;
    private ProvisionManagerImpl provisionManager;
    private DelegatingTransferProcessManager processManager;
    private TransferProcessStore transferProcessStore;

    @Override
    public String name() {
        return "Core Transfer";
    }

    @Override
    public Set<String> provides() {
        return Set.of("dataspaceconnector:statuschecker", "dataspaceconnector:dispatcher", "dataspaceconnector:manifestgenerator",
                "dataspaceconnector:transfer-process-manager", "dataspaceconnector:transfer-process-observable", DataProxyManager.FEATURE, ProxyEntryHandlerRegistry.FEATURE);
    }

    @Override
    public Set<String> requires() {
        return Set.of(TransferProcessStore.FEATURE);
    }

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
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

        var dataProxyRegistry = new DataProxyManagerImpl();
        context.registerService(DataProxyManager.class, dataProxyRegistry);


        transferProcessStore = context.getService(TransferProcessStore.class);
        var asyncMgr = AsyncTransferProcessManager.Builder.newInstance()
                .waitStrategy(waitStrategy)
                .manifestGenerator(manifestGenerator)
                .dataFlowManager(dataFlowManager)
                .provisionManager(provisionManager)
                .dispatcherRegistry(dispatcherRegistry)
                .statusCheckerRegistry(statusCheckerRegistry)
                .monitor(monitor)
                .build();

        var proxyEntryHandlerRegistry = new DefaultProxyEntryHandlerRegistry();
        context.registerService(ProxyEntryHandlerRegistry.class, proxyEntryHandlerRegistry);

        var syncMgr = new SyncTransferProcessManager(dataProxyRegistry, transferProcessStore, dispatcherRegistry, proxyEntryHandlerRegistry, typeManager);

        processManager = new DelegatingTransferProcessManager(asyncMgr, syncMgr);

        context.registerService(TransferProcessManager.class, processManager);
        context.registerService(TransferProcessObservable.class, asyncMgr);

    }

    @Override
    public void start() {


        provisionManager.start(transferProcessStore);
        processManager.start(transferProcessStore);
    }

    @Override
    public void shutdown() {
        if (processManager != null) {
            processManager.stop();
        }
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(DataRequest.class);
    }

}
