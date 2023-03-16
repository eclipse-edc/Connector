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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.transfer;

import org.eclipse.edc.connector.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.transfer.command.handlers.AddProvisionedResourceCommandHandler;
import org.eclipse.edc.connector.transfer.command.handlers.DeprovisionCompleteCommandHandler;
import org.eclipse.edc.connector.transfer.edr.EndpointDataReferenceReceiverRegistryImpl;
import org.eclipse.edc.connector.transfer.edr.EndpointDataReferenceTransformerRegistryImpl;
import org.eclipse.edc.connector.transfer.flow.DataFlowManagerImpl;
import org.eclipse.edc.connector.transfer.listener.TransferProcessEventListener;
import org.eclipse.edc.connector.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.transfer.process.StatusCheckerRegistryImpl;
import org.eclipse.edc.connector.transfer.process.TransferProcessManagerImpl;
import org.eclipse.edc.connector.transfer.provision.ProvisionManagerImpl;
import org.eclipse.edc.connector.transfer.provision.ResourceManifestGeneratorImpl;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.connector.transfer.spi.retry.TransferWaitStrategy;
import org.eclipse.edc.connector.transfer.spi.status.StatusCheckerRegistry;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.connector.transfer.spi.types.command.TransferProcessCommand;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.CoreExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.command.BoundedCommandQueue;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.command.CommandRunner;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.statemachine.retry.EntitySendRetryManagerConfiguration;

import java.time.Clock;

/**
 * Provides core data transfer services to the system.
 */
@CoreExtension
@Provides({ StatusCheckerRegistry.class, ResourceManifestGenerator.class, TransferProcessManager.class,
        TransferProcessObservable.class, DataFlowManager.class, ProvisionManager.class,
        EndpointDataReferenceReceiverRegistry.class, EndpointDataReferenceTransformerRegistry.class })
@Extension(value = TransferCoreExtension.NAME)
public class TransferCoreExtension implements ServiceExtension {

    public static final String NAME = "Transfer Core";

    public static final long DEFAULT_ITERATION_WAIT = 1000;
    public static final int DEFAULT_BATCH_SIZE = 20;
    public static final int DEFAULT_SEND_RETRY_LIMIT = 7;
    public static final long DEFAULT_SEND_RETRY_BASE_DELAY = 1000L;

    @Setting(value = "the iteration wait time in milliseconds in the transfer process state machine. Default value " + DEFAULT_ITERATION_WAIT, type = "long")
    private static final String TRANSFER_STATE_MACHINE_ITERATION_WAIT_MILLIS = "edc.transfer.state-machine.iteration-wait-millis";

    @Setting(value = "the batch size in the transfer process state machine. Default value " + DEFAULT_BATCH_SIZE, type = "int")
    private static final String TRANSFER_STATE_MACHINE_BATCH_SIZE = "edc.transfer.state-machine.batch-size";

    @Setting(value = "how many times a specific operation must be tried before terminating the transfer with error", type = "int", defaultValue = DEFAULT_SEND_RETRY_LIMIT + "")
    private static final String TRANSFER_SEND_RETRY_LIMIT = "edc.transfer.send.retry.limit";

    @Setting(value = "The base delay for retry mechanism in millisecond", type = "long", defaultValue = DEFAULT_SEND_RETRY_BASE_DELAY + "")
    private static final String TRANSFER_SEND_RETRY_BASE_DELAY_MS = "edc.transfer.send.retry.base-delay.ms";

    @Inject
    private TransferProcessStore transferProcessStore;

    @Inject
    private PolicyArchive policyArchive;

    @Inject
    private CommandHandlerRegistry registry;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Inject
    private DataAddressResolver addressResolver;

    @Inject
    private Vault vault;

    @Inject
    private EventRouter eventRouter;

    @Inject
    private Clock clock;

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private TypeManager typeManager;

    @Inject
    private Telemetry telemetry;

    private TransferProcessManagerImpl processManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        registerTypes(typeManager);

        var dataFlowManager = new DataFlowManagerImpl();
        context.registerService(DataFlowManager.class, dataFlowManager);

        var manifestGenerator = new ResourceManifestGeneratorImpl(policyEngine);
        context.registerService(ResourceManifestGenerator.class, manifestGenerator);

        var statusCheckerRegistry = new StatusCheckerRegistryImpl();
        context.registerService(StatusCheckerRegistry.class, statusCheckerRegistry);

        var provisionManager = new ProvisionManagerImpl(monitor);
        context.registerService(ProvisionManager.class, provisionManager);

        var iterationWaitMillis = context.getSetting(TRANSFER_STATE_MACHINE_ITERATION_WAIT_MILLIS, DEFAULT_ITERATION_WAIT);
        var waitStrategy = context.hasService(TransferWaitStrategy.class) ? context.getService(TransferWaitStrategy.class) : new ExponentialWaitStrategy(iterationWaitMillis);

        var endpointDataReferenceReceiverRegistry = new EndpointDataReferenceReceiverRegistryImpl();
        context.registerService(EndpointDataReferenceReceiverRegistry.class, endpointDataReferenceReceiverRegistry);

        // Register a default EndpointDataReferenceTransformer that can be overridden in extensions.
        var endpointDataReferenceTransformerRegistry = new EndpointDataReferenceTransformerRegistryImpl();
        context.registerService(EndpointDataReferenceTransformerRegistry.class, endpointDataReferenceTransformerRegistry);

        var commandQueue = new BoundedCommandQueue<TransferProcessCommand>(10);
        var observable = new TransferProcessObservableImpl();
        context.registerService(TransferProcessObservable.class, observable);

        observable.registerListener(new TransferProcessEventListener(eventRouter, clock));

        var retryLimit = context.getSetting(TRANSFER_SEND_RETRY_LIMIT, DEFAULT_SEND_RETRY_LIMIT);
        var retryBaseDelay = context.getSetting(TRANSFER_SEND_RETRY_BASE_DELAY_MS, DEFAULT_SEND_RETRY_BASE_DELAY);
        var sendRetryManagerConfiguration = new EntitySendRetryManagerConfiguration(retryLimit, () -> new ExponentialWaitStrategy(retryBaseDelay));

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
                .clock(clock)
                .typeManager(typeManager)
                .commandQueue(commandQueue)
                .commandRunner(new CommandRunner<>(registry, monitor))
                .observable(observable)
                .transferProcessStore(transferProcessStore)
                .policyArchive(policyArchive)
                .batchSize(context.getSetting(TRANSFER_STATE_MACHINE_BATCH_SIZE, DEFAULT_BATCH_SIZE))
                .addressResolver(addressResolver)
                .sendRetryManagerConfiguration(sendRetryManagerConfiguration)
                .build();

        context.registerService(TransferProcessManager.class, processManager);

        registry.register(new AddProvisionedResourceCommandHandler(processManager));
        registry.register(new DeprovisionCompleteCommandHandler(processManager));
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
        typeManager.registerTypes(ProvisionedContentResource.class);
        typeManager.registerTypes(DeprovisionedResource.class);
    }
}
