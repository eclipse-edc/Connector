/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.dataspaceconnector.transfer.core;

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.command.BoundedCommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandHandlerRegistry;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.retry.ExponentialWaitStrategy;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.CoreExtension;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessObservable;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.retry.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.TransferProcessCommand;
import org.eclipse.dataspaceconnector.transfer.core.command.handlers.AddProvisionedResourceCommandHandler;
import org.eclipse.dataspaceconnector.transfer.core.edr.DefaultEndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.transfer.core.edr.EndpointDataReferenceReceiverRegistryImpl;
import org.eclipse.dataspaceconnector.transfer.core.flow.DataFlowManagerImpl;
import org.eclipse.dataspaceconnector.transfer.core.inline.DataOperatorRegistryImpl;
import org.eclipse.dataspaceconnector.transfer.core.observe.TransferProcessObservableImpl;
import org.eclipse.dataspaceconnector.transfer.core.provision.ProvisionManagerImpl;
import org.eclipse.dataspaceconnector.transfer.core.provision.ResourceManifestGeneratorImpl;
import org.eclipse.dataspaceconnector.transfer.core.transfer.StatusCheckerRegistryImpl;
import org.eclipse.dataspaceconnector.transfer.core.transfer.TransferProcessManagerImpl;

/**
 * Provides core data transfer services to the system.
 */
@CoreExtension
@Provides({ StatusCheckerRegistry.class, ResourceManifestGenerator.class, TransferProcessManager.class,
        TransferProcessObservable.class, DataOperatorRegistry.class, DataFlowManager.class, ProvisionManager.class,
        EndpointDataReferenceReceiverRegistry.class, EndpointDataReferenceTransformer.class })
public class CoreTransferExtension implements ServiceExtension {
    private static final long DEFAULT_ITERATION_WAIT = 5000; // millis

    @EdcSetting
    private static final String TRANSFER_STATE_MACHINE_BATCH_SIZE = "edc.transfer.state-machine.batch-size";

    @Inject
    private TransferProcessStore transferProcessStore;
    @Inject
    private CommandHandlerRegistry registry;
    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    private TransferProcessManagerImpl processManager;

    @Override
    public String name() {
        return "Core Transfer";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var telemetry = context.getTelemetry();

        var typeManager = context.getTypeManager();

        registerTypes(typeManager);

        var dataFlowManager = new DataFlowManagerImpl();
        context.registerService(DataFlowManager.class, dataFlowManager);

        var manifestGenerator = new ResourceManifestGeneratorImpl();
        context.registerService(ResourceManifestGenerator.class, manifestGenerator);

        var statusCheckerRegistry = new StatusCheckerRegistryImpl();
        context.registerService(StatusCheckerRegistry.class, statusCheckerRegistry);

        var dataOperatorRegistry = new DataOperatorRegistryImpl();
        context.registerService(DataOperatorRegistry.class, dataOperatorRegistry);

        var vault = context.getService(Vault.class);

        var provisionManager = new ProvisionManagerImpl(monitor);
        context.registerService(ProvisionManager.class, provisionManager);

        var waitStrategy = context.hasService(TransferWaitStrategy.class) ? context.getService(TransferWaitStrategy.class) : new ExponentialWaitStrategy(DEFAULT_ITERATION_WAIT);

        var endpointDataReferenceReceiverRegistry = new EndpointDataReferenceReceiverRegistryImpl();
        context.registerService(EndpointDataReferenceReceiverRegistry.class, endpointDataReferenceReceiverRegistry);

        // Register a default EndpointDataReferenceTransformer that can be overridden in extensions.
        var endpointDataReferenceTransformer = new DefaultEndpointDataReferenceTransformer();
        context.registerService(EndpointDataReferenceTransformer.class, endpointDataReferenceTransformer);

        var commandQueue = new BoundedCommandQueue<TransferProcessCommand>(10);
        var observable = new TransferProcessObservableImpl();
        context.registerService(TransferProcessObservable.class, observable);

        processManager = TransferProcessManagerImpl.Builder.newInstance()
                .waitStrategy(waitStrategy)
                .manifestGenerator(manifestGenerator)
                .dataFlowManager(dataFlowManager)
                .provisionManager(provisionManager)
                .dispatcherRegistry(dispatcherRegistry)
                .statusCheckerRegistry(statusCheckerRegistry)
                .monitor(monitor)
                .telemetry(telemetry)
                .executorInstrumentation(context.getService(ExecutorInstrumentation.class))
                .vault(vault)
                .typeManager(typeManager)
                .commandQueue(commandQueue)
                .commandRunner(new CommandRunner<>(registry, monitor))
                .observable(observable)
                .store(transferProcessStore)
                .batchSize(context.getSetting(TRANSFER_STATE_MACHINE_BATCH_SIZE, 5))
                .build();

        context.registerService(TransferProcessManager.class, processManager);

        registry.register(new AddProvisionedResourceCommandHandler(processManager));
    }

    @Override
    public void start() {
        processManager.start();
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
