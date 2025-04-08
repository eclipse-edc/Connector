/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.framework;

import org.eclipse.edc.connector.dataplane.framework.manager.DataPlaneManagerImpl;
import org.eclipse.edc.connector.dataplane.framework.registry.TransferServiceRegistryImpl;
import org.eclipse.edc.connector.dataplane.framework.registry.TransferServiceSelectionStrategy;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.port.TransferProcessApiClient;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.concurrent.Executors;

import static org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager.DEFAULT_FLOW_LEASE_FACTOR;
import static org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager.DEFAULT_FLOW_LEASE_TIME;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_BATCH_SIZE;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_ITERATION_WAIT;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_SEND_RETRY_BASE_DELAY;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_SEND_RETRY_LIMIT;

/**
 * Provides core services for the Data Plane Framework.
 */
@Provides({ DataPlaneManager.class, TransferServiceRegistry.class })
@Extension(value = DataPlaneFrameworkExtension.NAME)
public class DataPlaneFrameworkExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Framework";
    private static final int DEFAULT_TRANSFER_THREADS = 20;

    @Setting(
            description = "the iteration wait time in milliseconds in the data plane state machine.",
            defaultValue = DEFAULT_ITERATION_WAIT + "",
            key = "edc.dataplane.state-machine.iteration-wait-millis")
    private long iterationWaitMillis;

    @Setting(
            description = "the batch size in the data plane state machine.",
            defaultValue = DEFAULT_BATCH_SIZE + "",
            key = "edc.dataplane.state-machine.batch-size"
    )
    private int batchSize;

    @Setting(
            description = "how many times a specific operation must be tried before terminating the dataplane with error",
            defaultValue = DEFAULT_SEND_RETRY_LIMIT + "",
            key = "edc.dataplane.send.retry.limit"
    )
    private int sendRetryLimit;

    @Setting(
            description = "The base delay for the dataplane retry mechanism in millisecond",
            defaultValue = DEFAULT_SEND_RETRY_BASE_DELAY + "",
            key = "edc.dataplane.send.retry.base-delay.ms"
    )
    private long sendRetryBaseDelay;

    @Setting(
            description = "Size of the transfer thread pool. It is advisable to set it bigger than the state machine batch size",
            defaultValue = DEFAULT_TRANSFER_THREADS + "",
            key = "edc.dataplane.transfer.threads"
    )
    private int numThreads;

    @Configuration
    private FlowLeaseConfiguration flowLeaseConfiguration;

    private DataPlaneManagerImpl dataPlaneManager;

    @Inject
    private TransferServiceSelectionStrategy transferServiceSelectionStrategy;
    @Inject
    private DataPlaneStore store;
    @Inject
    private TransferProcessApiClient transferProcessApiClient;
    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    @Inject
    private Telemetry telemetry;
    @Inject
    private Clock clock;
    @Inject
    private PipelineService pipelineService;
    @Inject
    private DataPlaneAuthorizationService authorizationService;
    @Inject
    private ResourceDefinitionGeneratorManager resourceDefinitionGeneratorManager;
    @Inject
    private ProvisionerManager provisionerManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var transferServiceRegistry = new TransferServiceRegistryImpl(transferServiceSelectionStrategy);
        transferServiceRegistry.registerTransferService(pipelineService);
        context.registerService(TransferServiceRegistry.class, transferServiceRegistry);

        var waitStrategy = new ExponentialWaitStrategy(iterationWaitMillis);

        dataPlaneManager = DataPlaneManagerImpl.Builder.newInstance()
                .waitStrategy(waitStrategy)
                .batchSize(batchSize)
                .clock(clock)
                .entityRetryProcessConfiguration(getEntityRetryProcessConfiguration())
                .executorInstrumentation(executorInstrumentation)
                .authorizationService(authorizationService)
                .transferServiceRegistry(transferServiceRegistry)
                .store(store)
                .transferProcessClient(transferProcessApiClient)
                .monitor(monitor)
                .telemetry(telemetry)
                .runtimeId(context.getRuntimeId())
                .flowLeaseConfiguration(flowLeaseConfiguration)
                .resourceDefinitionGeneratorManager(resourceDefinitionGeneratorManager)
                .provisionerManager(provisionerManager)
                .build();

        context.registerService(DataPlaneManager.class, dataPlaneManager);
    }

    @Override
    public void start() {
        dataPlaneManager.restartFlows();
        dataPlaneManager.start();
    }

    @Override
    public void shutdown() {
        if (dataPlaneManager != null) {
            dataPlaneManager.stop();
        }
        pipelineService.closeAll();
    }

    @Provider
    public DataTransferExecutorServiceContainer dataTransferExecutorServiceContainer(ServiceExtensionContext context) {
        var executorService = Executors.newFixedThreadPool(numThreads);
        return new DataTransferExecutorServiceContainer(
                executorInstrumentation.instrument(executorService, "Data plane transfers"));
    }

    @Settings
    public record FlowLeaseConfiguration(
            @Setting(
                    key = "edc.dataplane.state-machine.flow.lease.time",
                    description = "The time in milliseconds after which a runtime renews its ownership on a started data flow.",
                    defaultValue = DEFAULT_FLOW_LEASE_TIME + "")
            long time,
            @Setting(
                    key = "edc.dataplane.state-machine.flow.lease.factor",
                    description = "After flow lease time * factor a started data flow will be considered abandoned by the owner and so another runtime can caught it up and start it again.",
                    defaultValue = DEFAULT_FLOW_LEASE_FACTOR + "")
            int factor
    ) {

        public FlowLeaseConfiguration() {
            this(DEFAULT_FLOW_LEASE_TIME, DEFAULT_FLOW_LEASE_FACTOR);
        }

        /**
         * After this time has passed, a DataFlow can be considered "abandoned" and it can be picked up by another runtime.
         *
         * @return the abandoned time.
         */
        public long abandonTime() {
            return time * factor;
        }

    }

    @NotNull
    private EntityRetryProcessConfiguration getEntityRetryProcessConfiguration() {
        return new EntityRetryProcessConfiguration(sendRetryLimit, () -> new ExponentialWaitStrategy(sendRetryBaseDelay));
    }
}
