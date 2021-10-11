package org.eclipse.dataspaceconnector.catalog.cache.crawler;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;
import static net.jodah.failsafe.Failsafe.with;

public class CrawlerImpl implements Crawler {

    private final ProtocolAdapterRegistry protocolAdapterRegistry;
    private final Monitor monitor;
    private final BlockingQueue<UpdateResponse> queue;
    private final RetryPolicy<Object> retryPolicy;
    private final WorkItemQueue workItems;
    private final Duration workQueuePollTimeout;
    private final ReentrantLock lock;
    private final AtomicBoolean isActive;

    CrawlerImpl(WorkItemQueue workItems, Monitor monitor, BlockingQueue<UpdateResponse> responseQueue, RetryPolicy<Object> retryPolicy, ProtocolAdapterRegistry protocolAdapterRegistry, Duration workQueuePollTimeout) {
        this.workItems = workItems;
        this.protocolAdapterRegistry = protocolAdapterRegistry;
        this.monitor = monitor;
        queue = responseQueue;
        this.retryPolicy = retryPolicy;
        this.workQueuePollTimeout = workQueuePollTimeout;
        lock = new ReentrantLock();
        isActive = new AtomicBoolean(true);
    }


    @Override
    public void run() {

        while (isActive.get()) {
            lock.lock();
            WorkItem item = null;
            try {
                item = workItems.poll(workQueuePollTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                handleError(item, e.getMessage());
            }

            monitor.debug(item == null ? "Nothing to do" : "WorkItem acquired");
            if (item != null) {
                // search for an adapter
                var adapters = protocolAdapterRegistry.findForProtocol(item.getProtocol());

                if (adapters.isEmpty()) {
                    // otherwise error out the workitem
                    handleError(item, "No Adapter found for protocol " + item.getProtocol());
                } else {
                    // if the adapters are found, use them to send the update request
                    WorkItem finalItem = item;
                    adapters.forEach(a -> a.sendRequest(new UpdateRequest(finalItem.getUrl()))
                            .whenComplete((updateResponse, throwable) -> {
                                if (throwable != null) {
                                    handleError(finalItem, throwable.getMessage());
                                } else {
                                    handleResponse(updateResponse);
                                }
                            }));
                }
            }
            lock.unlock();
        }
    }

    @Override
    public CompletableFuture<Void> join() {
        return join(10, TimeUnit.SECONDS);
    }

    public CompletableFuture<Void> join(long timeout, TimeUnit unit) {
        return CompletableFuture.supplyAsync(() -> {
            monitor.debug("Stopping Crawler");
            isActive.set(false);
            try {
                return lock.tryLock(timeout, unit);
            } catch (InterruptedException e) {
                return false;
            } finally {
                lock.unlock();
            }
        }).thenCompose(bool -> bool ? CompletableFuture.completedFuture(null) : CompletableFuture.failedFuture(new RuntimeException("")));
    }


    private void handleError(@Nullable WorkItem errorWorkItem, String message) {
        monitor.severe(message);

        if (errorWorkItem != null) {
            errorWorkItem.error(message);
            //todo: re-enqueue the workitem?
            //workItems.offer(errorWorkItem);
        }
    }

    private void handleResponse(UpdateResponse updateResponse) {
        monitor.info(format("update-response received: %s", updateResponse.toString()));
        var offered = with(retryPolicy).get(() -> queue.offer(updateResponse));
        if (!offered) {
            monitor.severe("Inserting update-response into queue failed due to timeout!");
        }
    }


    public static final class Builder {
        private ProtocolAdapterRegistry adapters;
        private Monitor monitor;
        private BlockingQueue<UpdateResponse> queue;
        private RetryPolicy<Object> retryPolicy;
        private WorkItemQueue workItems;
        private Duration workQueuePollTimeout;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocolAdapters(ProtocolAdapterRegistry adapters) {
            this.adapters = adapters;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public Builder workQueuePollTimeout(Duration duration) {
            workQueuePollTimeout = duration;
            return this;
        }

        public Builder queue(BlockingQueue<UpdateResponse> queue) {
            this.queue = queue;
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
            return new CrawlerImpl(workItems, monitor, queue, retryPolicy, adapters, workQueuePollTimeout);
        }
    }
}
