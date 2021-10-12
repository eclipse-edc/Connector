package org.eclipse.dataspaceconnector.catalog.cache.crawler;

import info.schnatterer.mobynamesgenerator.MobyNamesGenerator;
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
import java.util.function.Supplier;

import static java.lang.String.format;
import static net.jodah.failsafe.Failsafe.with;

public class CrawlerImpl implements Crawler {

    private final ProtocolAdapterRegistry protocolAdapterRegistry;
    private final Monitor monitor;
    private final BlockingQueue<UpdateResponse> queue;
    private final RetryPolicy<Object> retryPolicy;
    private final WorkItemQueue workItemQueue;
    private final Supplier<Duration> workQueuePollTimeout;
    private final AtomicBoolean isActive;
    private final String crawlerId;

    CrawlerImpl(WorkItemQueue workItemQueue, Monitor monitor, BlockingQueue<UpdateResponse> responseQueue, RetryPolicy<Object> retryPolicy, ProtocolAdapterRegistry protocolAdapterRegistry, Supplier<Duration> workQueuePollTimeout) {
        this.workItemQueue = workItemQueue;
        this.protocolAdapterRegistry = protocolAdapterRegistry;
        this.monitor = monitor;
        queue = responseQueue;
        this.retryPolicy = retryPolicy;
        this.workQueuePollTimeout = workQueuePollTimeout;
        isActive = new AtomicBoolean(true);
        crawlerId = format("\"%s\"", MobyNamesGenerator.getRandomName().replace("_", " "));
    }


    @Override
    public void run() {

        while (isActive.get()) {
            workItemQueue.lock();
            WorkItem item = null;
            try {
                item = workItemQueue.poll(workQueuePollTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                handleError(item, e.getMessage());
            }

            if (item != null)
                monitor.debug(format("%s: WorkItem acquired", crawlerId));
            if (item != null) {
                // search for an adapter
                var adapters = protocolAdapterRegistry.findForProtocol(item.getProtocol());

                if (adapters == null || adapters.isEmpty()) {
                    // otherwise error out the workitem
                    handleError(item, crawlerId + ": No Adapter found for protocol " + item.getProtocol());
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
            workItemQueue.unlock();
        }
    }

    @Override
    public CompletableFuture<Void> join() {
        return join(10, TimeUnit.SECONDS);
    }

    public CompletableFuture<Void> join(long timeout, TimeUnit unit) {
        return CompletableFuture.supplyAsync(() -> {
            monitor.debug(crawlerId + ": Stopping");
            isActive.set(false);
            try {
                return workItemQueue.tryLock(timeout, unit);
            } finally {
                workItemQueue.unlock();
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
        monitor.info(format("%s: update-response received: %s", crawlerId, updateResponse.toString()));
        var offered = with(retryPolicy).get(() -> queue.offer(updateResponse));
        if (!offered) {
            monitor.severe(crawlerId + ": Inserting update-response into queue failed due to timeout!");
        }
    }


    public static final class Builder {
        private ProtocolAdapterRegistry adapters;
        private Monitor monitor;
        private BlockingQueue<UpdateResponse> queue;
        private RetryPolicy<Object> retryPolicy;
        private WorkItemQueue workItems;
        private Supplier<Duration> workQueuePollTimeout;

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

        public Builder workQueuePollTimeout(Supplier<Duration> duration) {
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
