package org.eclipse.dataspaceconnector.catalog.cache.crawler;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static net.jodah.failsafe.Failsafe.with;

public class CrawlerImpl implements Crawler {

    private final List<ProtocolAdapter> adapters;
    private final Monitor monitor;
    private final BlockingQueue<UpdateResponse> queue;
    private final RetryPolicy<Object> retryPolicy;
    private final WorkItemQueue workItems;
    private final Duration waitForWorkItem;
    private final ReentrantLock lock;
    private final AtomicBoolean isActive;

    CrawlerImpl(WorkItemQueue workItems, Monitor monitor, BlockingQueue<UpdateResponse> responseQueue, RetryPolicy<Object> retryPolicy, List<ProtocolAdapter> adapters, Duration waitForWorkItem) {
        this.workItems = workItems;
        this.adapters = adapters;
        this.monitor = monitor;
        queue = responseQueue;
        this.retryPolicy = retryPolicy;
        this.waitForWorkItem = waitForWorkItem;
        lock = new ReentrantLock();
        isActive = new AtomicBoolean(true);
    }


    @Override
    public void run() {

        while (isActive.get()) {
            lock.lock();
            WorkItem item = null;
            try {
                item = workItems.poll(waitForWorkItem.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                handleError(item, e.getMessage());
            }

            monitor.debug(item == null ? "Nothing to do" : "WorkItem acquired");
            if (item != null) {
                // search for an adapter
                var adapters = getMatchingAdapters(item, this.adapters);

                if (adapters.isEmpty()) {
                    // otherwise error out the workitem);
                    handleError(item, "No Adapter found for protocol " + item.getProtocolType());
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
    public void addAdapter(ProtocolAdapter adapter) {
        adapters.add(adapter);
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

    private List<ProtocolAdapter> getMatchingAdapters(WorkItem item, List<ProtocolAdapter> adapters) {
        return adapters.stream().filter(adp -> adp.getClass().equals(item.getProtocolType())).collect(Collectors.toList());
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
        private List<ProtocolAdapter> adapters = new ArrayList<>();
        private Monitor monitor;
        private BlockingQueue<UpdateResponse> queue;
        private RetryPolicy<Object> retryPolicy;
        private WorkItemQueue workItems;
        private Duration waitForWorkItem;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder adapters(List<ProtocolAdapter> adapters) {
            this.adapters = adapters;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public Builder waitItemTime(Duration duration) {
            waitForWorkItem = duration;
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
            return new CrawlerImpl(workItems, monitor, queue, retryPolicy, adapters, waitForWorkItem);
        }
    }
}
