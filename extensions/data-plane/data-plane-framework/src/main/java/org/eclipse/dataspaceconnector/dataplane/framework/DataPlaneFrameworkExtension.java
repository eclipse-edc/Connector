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

package org.eclipse.dataspaceconnector.dataplane.framework;

import org.eclipse.dataspaceconnector.dataplane.framework.manager.DataPlaneManagerImpl;
import org.eclipse.dataspaceconnector.dataplane.framework.pipeline.PipelineServiceImpl;
import org.eclipse.dataspaceconnector.dataplane.framework.pipeline.PipelineServiceTransferServiceImpl;
import org.eclipse.dataspaceconnector.dataplane.framework.registry.TransferServiceRegistryImpl;
import org.eclipse.dataspaceconnector.dataplane.framework.registry.TransferServiceSelectionStrategy;
import org.eclipse.dataspaceconnector.dataplane.framework.store.InMemoryDataPlaneStore;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSinkFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.dataspaceconnector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * Provides core services for the Data Plane Framework.
 */
@Provides({DataPlaneManager.class, PipelineService.class, DataTransferExecutorServiceContainer.class})
public class DataPlaneFrameworkExtension implements ServiceExtension {
    private static final int IN_MEMORY_STORE_CAPACITY = 1000;

    @EdcSetting
    private static final String QUEUE_CAPACITY = "edc.dataplane.queue.capacity";
    private static final int DEFAULT_QUEUE_CAPACITY = 10000;

    @EdcSetting
    private static final String WORKERS = "edc.dataplane.workers";
    private static final int DEFAULT_WORKERS = 10;

    @EdcSetting
    private static final String WAIT_TIMEOUT = "edc.dataplane.wait";
    private static final long DEFAULT_WAIT_TIMEOUT = 1000;

    @EdcSetting
    private static final String TRANSFER_THREADS = "edc.dataplane.transfer.threads";
    private static final int DEFAULT_TRANSFER_THREADS = 10;

    private ServiceExtensionContext context;

    private DataPlaneManagerImpl dataPlaneManager;
    private Monitor monitor;

    @Inject(required = false)
    private TransferServiceSelectionStrategy transferServiceSelectionStrategy;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    @Override
    public String name() {
        return "Data Plane Framework";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.context = context;
        var pipelineService = new PipelineServiceImpl();
        pipelineService.registerFactory(new OutputStreamDataSinkFactory()); // Added by default to support synchronous data transfer, i.e. pull data
        context.registerService(PipelineService.class, pipelineService);
        var transferService = new PipelineServiceTransferServiceImpl(pipelineService);

        var transferServiceRegistry = new TransferServiceRegistryImpl(Objects.requireNonNullElseGet(transferServiceSelectionStrategy,
                TransferServiceSelectionStrategy::selectFirst));
        transferServiceRegistry.registerTransferService(transferService);
        context.registerService(TransferServiceRegistry.class, transferServiceRegistry);

        var numThreads = context.getSetting(TRANSFER_THREADS, DEFAULT_TRANSFER_THREADS);
        var executorService = Executors.newFixedThreadPool(numThreads);
        var executorContainer = new DataTransferExecutorServiceContainer(
                executorInstrumentation.instrument(executorService, "Data plane transfers"));
        context.registerService(DataTransferExecutorServiceContainer.class, executorContainer);

        monitor = context.getMonitor();
        var telemetry = context.getTelemetry();

        var queueCapacity = context.getSetting(QUEUE_CAPACITY, DEFAULT_QUEUE_CAPACITY);
        var workers = context.getSetting(WORKERS, DEFAULT_WORKERS);
        var waitTimeout = context.getSetting(WAIT_TIMEOUT, DEFAULT_WAIT_TIMEOUT);

        dataPlaneManager = DataPlaneManagerImpl.Builder.newInstance()
                .queueCapacity(queueCapacity)
                .executorInstrumentation(executorInstrumentation)
                .workers(workers)
                .waitTimeout(waitTimeout)
                .pipelineService(pipelineService)
                .transferServiceRegistry(transferServiceRegistry)
                .store(new InMemoryDataPlaneStore(IN_MEMORY_STORE_CAPACITY))
                .monitor(monitor)
                .telemetry(telemetry)
                .build();

        context.registerService(DataPlaneManager.class, dataPlaneManager);
    }

    @Override
    public void start() {
        if (!context.hasService(DataPlaneStore.class)) {
            monitor.info("Registering in-memory Data Plane store.");
            context.registerService(DataPlaneStore.class, new InMemoryDataPlaneStore(IN_MEMORY_STORE_CAPACITY));
        }
        dataPlaneManager.start();
    }

    @Override
    public void shutdown() {
        if (dataPlaneManager != null) {
            dataPlaneManager.forceStop();
        }
    }
}
