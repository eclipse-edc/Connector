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

package org.eclipse.dataspaceconnector.dataplane.framework.manager;

import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.dataspaceconnector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.dataspaceconnector.dataplane.spi.store.DataPlaneStore.State;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default data manager implementation.
 * <p>
 * This implementation uses a simple bounded queue to support backpressure when the system is overloaded. This should support sufficient performance since data transfers
 * generally do not require low-latency. If low-latency operation becomes a requirement, a concurrent queuing mechanism can be used.
 */
public class DataPlaneManagerImpl implements DataPlaneManager {
    private int queueCapacity = 10000;
    private int workers = 1;
    private long waitTimeout = 100;

    private PipelineService pipelineService;
    private ExecutorInstrumentation executorInstrumentation;
    private Monitor monitor;

    private BlockingQueue<DataFlowRequest> queue;
    private ExecutorService executorService;

    private AtomicBoolean active = new AtomicBoolean();
    private DataPlaneStore store;
    private TransferServiceRegistry transferServiceRegistry;

    public void start() {
        queue = new ArrayBlockingQueue<>(queueCapacity);
        active.set(true);
        executorService = executorInstrumentation.instrument(Executors.newFixedThreadPool(workers), getClass().getSimpleName());
        for (var i = 0; i < workers; i++) {
            executorService.submit(this::run);
        }
    }

    public void stop() {
        active.set(false);
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    public void forceStop() {
        active.set(false);
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    public Result<Boolean> validate(DataFlowRequest dataRequest) {
        var transferService = transferServiceRegistry.resolveTransferService(dataRequest);
        return transferService != null ?
                transferService.validate(dataRequest) :
                Result.failure("Cannot handle this request");
    }

    public void initiateTransfer(DataFlowRequest dataRequest) {
        queue.add(dataRequest);
        store.received(dataRequest.getProcessId());
    }

    @Override
    public CompletableFuture<StatusResult<Void>> transfer(DataSource source, DataFlowRequest request) {
        return pipelineService.transfer(source, request);
    }

    @Override
    public CompletableFuture<StatusResult<Void>> transfer(DataSink sink, DataFlowRequest request) {
        return pipelineService.transfer(sink, request);
    }

    @Override
    public State transferState(String processId) {
        return store.getState(processId);
    }

    private void run() {
        while (active.get()) {
            DataFlowRequest request = null;
            try {
                request = queue.poll(waitTimeout, TimeUnit.MILLISECONDS);
                if (request == null) {
                    continue;
                }
                final var polledRequest = request;

                var transferService = transferServiceRegistry.resolveTransferService(polledRequest);
                if (transferService == null) {
                    // Should not happen since resolving a transferService is part of payload validation
                    // TODO persist error details
                    store.completed(polledRequest.getProcessId());
                } else {
                    transferService.transfer(request).whenComplete((result, exception) -> {
                        if (polledRequest.isTrackable()) {
                            // TODO persist TransferResult or error details
                            store.completed(polledRequest.getProcessId());
                        }
                    });
                }
            } catch (InterruptedException e) {
                Thread.interrupted();
                active.set(false);
                break;
            } catch (Exception e) {
                if (request == null) {
                    monitor.severe("Unable to dequeue data request", e);
                } else {
                    monitor.severe("Error processing data request: " + request.getProcessId(), e);
                    // TODO persist error details
                    store.completed(request.getProcessId());
                }
            }
        }
    }

    public static class Builder {
        private DataPlaneManagerImpl manager;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder pipelineService(PipelineService pipelineService) {
            manager.pipelineService = pipelineService;
            return this;
        }

        public Builder executorInstrumentation(ExecutorInstrumentation executorInstrumentation) {
            manager.executorInstrumentation = executorInstrumentation;
            return this;
        }

        public Builder transferServiceRegistry(TransferServiceRegistry transferServiceRegistry) {
            manager.transferServiceRegistry = transferServiceRegistry;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            manager.monitor = monitor;
            return this;
        }

        public Builder queueCapacity(int capacity) {
            manager.queueCapacity = capacity;
            return this;
        }

        public Builder workers(int workers) {
            manager.workers = workers;
            return this;
        }

        public Builder waitTimeout(long waitTimeout) {
            manager.waitTimeout = waitTimeout;
            return this;
        }

        public Builder store(DataPlaneStore store) {
            manager.store = store;
            return this;
        }

        public DataPlaneManagerImpl build() {
            return manager;
        }

        private Builder() {
            manager = new DataPlaneManagerImpl();
        }
    }

}
