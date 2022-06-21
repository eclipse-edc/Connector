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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.cache.crawler;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.CrawlerErrorHandler;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static java.lang.String.format;
import static net.jodah.failsafe.Failsafe.with;

public class CrawlerImpl implements Crawler {

    private final NodeQueryAdapterRegistry catalogQueryAdapterRegistry;
    private final Monitor monitor;
    private final BlockingQueue<UpdateResponse> updateResponseQueue;
    private final RetryPolicy<Object> updateResponseEnqueueRetryPolicy;
    private final WorkItemQueue workItemQueue;
    private final Supplier<Duration> workQueuePollTimeout;
    private final AtomicBoolean isActive;
    private final String crawlerId;
    private final CrawlerErrorHandler errorHandler;

    CrawlerImpl(WorkItemQueue workItemQueue, Monitor monitor, BlockingQueue<UpdateResponse> responseQueue,
                RetryPolicy<Object> updateResponseEnqueueRetryPolicy, NodeQueryAdapterRegistry catalogQueryAdapterRegistry,
                Supplier<Duration> workQueuePollTimeout, CrawlerErrorHandler errorHandler) {
        this.workItemQueue = workItemQueue;
        this.catalogQueryAdapterRegistry = catalogQueryAdapterRegistry;
        this.monitor = monitor;
        updateResponseQueue = responseQueue;
        this.updateResponseEnqueueRetryPolicy = updateResponseEnqueueRetryPolicy;
        this.workQueuePollTimeout = workQueuePollTimeout;
        this.errorHandler = errorHandler;
        isActive = new AtomicBoolean(true);
        crawlerId = format("\"Crawler-%s\"", UUID.randomUUID());
    }


    @Override
    public void run() {

        while (isActive.get()) {
            workItemQueue.lock();
            try {

                WorkItem item = null;
                try {
                    item = workItemQueue.poll(workQueuePollTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    monitor.warning(e.getMessage());
                }

                if (item != null) {
                    monitor.debug(format("%s: WorkItem acquired", crawlerId));

                    // search for an adapter
                    var adapters = catalogQueryAdapterRegistry.findForProtocol(item.getProtocol());

                    if (adapters.isEmpty()) {
                        // otherwise error out the workitem
                        handleError(item, String.format("%s: No Adapter found for protocol [%s :: %s]", crawlerId, item.getProtocol(), item.getUrl()));
                    } else {
                        // if the adapters are found, use them to send the update request
                        WorkItem finalItem = item;
                        for (NodeQueryAdapter a : adapters) {
                            a.sendRequest(new UpdateRequest(finalItem.getUrl()))
                                    // the following happens on a different thread
                                    .whenComplete((updateResponse, throwable) -> {
                                        if (throwable != null) {
                                            handleError(finalItem, throwable.getMessage());
                                        } else {
                                            handleResponse(updateResponse);
                                        }
                                    });
                        }

                    }
                }

            } catch (Throwable thr) {
                //runnables that run on an executor may swallow the exception
                monitor.severe(format("Unexpected exception happened during in crawler %s", crawlerId), thr);
                throw new EdcException(thr);
            } finally {
                workItemQueue.unlock();
                try {
                    Thread.sleep(5); //yield
                } catch (InterruptedException ignored) {
                    monitor.debug("interrupted during yielding");
                }
            }
        }
    }

    @Override
    public boolean join(long timeout, TimeUnit unit) {
        isActive.set(false);
        try {
            return workItemQueue.tryLock(timeout, unit);
        } finally {
            workItemQueue.unlock();
        }
    }

    private void handleError(@Nullable WorkItem errorWorkItem, String message) {
        monitor.severe(message);

        if (errorWorkItem != null) {
            errorWorkItem.error(message);
            errorHandler.accept(errorWorkItem);
        }
    }

    private void handleResponse(UpdateResponse updateResponse) {
        var offered = with(updateResponseEnqueueRetryPolicy).get(() -> updateResponseQueue.offer(updateResponse));
        if (!offered) {
            monitor.severe(crawlerId + ": Inserting update-response into queue failed due to timeout!");
            //todo: how to react?
        }
    }

    public static final class Builder {
        private NodeQueryAdapterRegistry adapters;
        private Monitor monitor;
        private BlockingQueue<UpdateResponse> queue;
        private RetryPolicy<Object> retryPolicy;
        private WorkItemQueue workItems;
        private Supplier<Duration> workQueuePollTimeout;
        private CrawlerErrorHandler errorHandler;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocolAdapters(NodeQueryAdapterRegistry adapters) {
            this.adapters = adapters;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public Builder workQueuePollTimeout(Supplier<Duration> duration) {
            workQueuePollTimeout = duration;
            return this;
        }

        public Builder queue(BlockingQueue<UpdateResponse> queue) {
            this.queue = queue;
            return this;
        }

        public Builder errorReceiver(CrawlerErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        public Builder retryPolicy(RetryPolicy<Object> retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder workItems(WorkItemQueue workItems) {
            this.workItems = workItems;
            return this;
        }

        public CrawlerImpl build() {
            Objects.requireNonNull(workItems);
            Objects.requireNonNull(queue);
            Objects.requireNonNull(adapters);

            return new CrawlerImpl(workItems, monitor, queue, retryPolicy, adapters, workQueuePollTimeout, errorHandler);
        }
    }
}
