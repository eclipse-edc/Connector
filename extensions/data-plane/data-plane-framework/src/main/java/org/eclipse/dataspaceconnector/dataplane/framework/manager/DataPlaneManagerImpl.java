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
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default data manager implementation.
 *
 * This implementation uses a simple bounded queue to support backpressure when the system is overloaded. This should support sufficient performance since data transfers
 * generally do not require low-latency. If low-latency operation becomes a requirement, a concurrent queuing mechanism can be used.
 */
public class DataPlaneManagerImpl implements DataPlaneManager {
    private int queueCapacity = 10000;
    private int workers = 1;
    private long waitTimeout = 100;

    private PipelineService pipelineService;
    private Monitor monitor;

    private BlockingQueue<DataFlowRequest> queue;
    private ExecutorService executorService;

    private AtomicBoolean active = new AtomicBoolean();

    public void start() {
        queue = new ArrayBlockingQueue<>(queueCapacity);
        active.set(true);
        executorService = Executors.newFixedThreadPool(workers);
        for (var i = 0; i < workers; i++) {
            executorService.submit(this::run);
        }
    }

    public void stop() {
        executorService.shutdownNow();
    }

    public void initiateTransfer(DataFlowRequest dataRequest) {
        queue.add(dataRequest);
    }

    private void run() {
        while (active.get()) {
            DataFlowRequest request = null;
            try {
                request = queue.poll(waitTimeout, TimeUnit.MILLISECONDS);
                if (request == null) {
                    continue;
                }
                pipelineService.transfer(request).whenComplete((result, exception) -> {
                    // TODO persist result
                });
            } catch (InterruptedException e) {
                Thread.interrupted();
                active.set(false);
                break;
            } catch (Exception e) {
                if (request == null) {
                    monitor.severe("Unable to dequeue data request", e);
                } else {
                    monitor.severe("Error processing data request: " + request.getProcessId(), e);
                }
                // TODO mark request in error
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

        public DataPlaneManagerImpl build() {
            return manager;
        }

        private Builder() {
            manager = new DataPlaneManagerImpl();
        }
    }

}
