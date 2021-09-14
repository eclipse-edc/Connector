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

package org.eclipse.dataspaceconnector.transfer.demo.protocols.common;

import org.eclipse.dataspaceconnector.spi.EdcException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements async resource provisioning.
 * This simulates the asynchronous nature of provisioning cloud storage and data topics.
 */
public abstract class AbstractQueuedProvisioner {

    private final AtomicBoolean active = new AtomicBoolean();

    private long provisionWait = 100;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final BlockingQueue<QueueEntry> queue = new LinkedBlockingQueue<>(10000);

    public void setProvisionWait(long milliseconds) {
        provisionWait = milliseconds;
    }

    public void start() {
        active.set(true);
        executorService.submit(this::run);
    }

    public void stop() {
        active.set(false);
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    public CompletableFuture<DataDestination> provision(String topicName) {
        CompletableFuture<DataDestination> future = new CompletableFuture<>();
        try {
            queue.put(new QueueEntry(topicName, future));
            return future;
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new EdcException(e);
        }
    }

    protected abstract void onEntry(QueueEntry entry);

    private void run() {
        while (active.get()) {
            try {
                var entry = queue.poll(provisionWait, TimeUnit.MILLISECONDS);
                if (entry != null) {
                    onEntry(entry);
                }
                Thread.sleep(provisionWait); // simulate async behavior
            } catch (InterruptedException e) {
                Thread.interrupted();
                throw new EdcException(e);
            }
        }
    }

    protected static class QueueEntry {
        private final String destinationName;
        private final CompletableFuture<DataDestination> future;

        public QueueEntry(String destinationName, CompletableFuture<DataDestination> future) {
            this.destinationName = destinationName;
            this.future = future;
        }

        public String getDestinationName() {
            return destinationName;
        }

        public CompletableFuture<DataDestination> getFuture() {
            return future;
        }
    }
}
