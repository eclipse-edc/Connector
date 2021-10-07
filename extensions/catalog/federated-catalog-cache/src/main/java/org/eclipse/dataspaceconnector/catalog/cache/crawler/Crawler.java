package org.eclipse.dataspaceconnector.catalog.cache.crawler;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.cache.spi.ProtocolAdapter;
import org.eclipse.dataspaceconnector.catalog.cache.spi.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.cache.spi.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import static java.lang.String.format;
import static net.jodah.failsafe.Failsafe.with;

public class Crawler implements Runnable {

    private final List<ProtocolAdapter> adapters;
    private final Monitor monitor;
    private final BlockingQueue<UpdateResponse> queue;
    private final RetryPolicy<Object> retryPolicy;
    private final List<String> targetUrls;

    public Crawler(List<String> targetUrls, List<ProtocolAdapter> adapters, Monitor monitor, BlockingQueue<UpdateResponse> queue, RetryPolicy<Object> retryPolicy) {
        this.targetUrls = targetUrls;
        this.adapters = adapters;
        this.monitor = monitor;
        this.queue = queue;
        this.retryPolicy = retryPolicy;
    }

    public List<String> getTargetUrls() {
        return targetUrls;
    }

    @Override
    public void run() {
        adapters.forEach(adapter -> {
            adapter.sendRequest(new UpdateRequest())
                    .whenComplete((updateResponse, throwable) -> {
                        if (throwable != null) {
                            handleError(throwable);
                        } else {
                            handleResponse(updateResponse);
                        }
                    });
        });
    }

    private void handleError(Throwable throwable) {
        monitor.severe("An update-request failed", throwable);
    }

    private void handleResponse(UpdateResponse updateResponse) {
        monitor.info(format("update-response received: %s", updateResponse.toString()));
        var offered = with(retryPolicy).get(() -> queue.offer(updateResponse));
        if (!offered) {
            monitor.severe("Inserting update-response into queue failed due to timeout!");
        }
    }


    public static final class Builder {
        private List<ProtocolAdapter> adapters;
        private Monitor monitor;
        private BlockingQueue<UpdateResponse> queue;
        private RetryPolicy<Object> retryPolicy;
        private List<String> targets;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder adapters(List<ProtocolAdapter> adapters) {
            this.adapters = adapters;
            return this;
        }

        public Builder targets(List<String> targets) {
            this.targets = targets;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            this.monitor = monitor;
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

        public Crawler build() {
            return new Crawler(targets, adapters, monitor, queue, retryPolicy);
        }
    }
}
