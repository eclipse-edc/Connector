package org.eclipse.dataspaceconnector.catalog.cache;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.cache.crawler.CrawlerImpl;
import org.eclipse.dataspaceconnector.catalog.cache.loader.LoaderManagerImpl;
import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.LoaderManager;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

public class FederatedCatalogCacheExtension implements ServiceExtension {
    private static final int DEFAULT_QUEUE_LENGTH = 50;
    private static final int DEFAULT_BATCH_SIZE = 5;
    private static final int DEFAULT_RETRY_TIMEOUT_MILLIS = 2000;

    @Override
    public Set<String> provides() {
        return Set.of(Crawler.FEATURE, LoaderManager.FEATURE);
    }

    @Override
    public Set<String> requires() {
        return Set.of("edc:retry-policy");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var queue = new ArrayBlockingQueue<UpdateResponse>(DEFAULT_QUEUE_LENGTH);
        @SuppressWarnings("unchecked")
        var retryPolicy = (RetryPolicy<Object>) context.getService(RetryPolicy.class);
        var crawler = CrawlerImpl.Builder.newInstance()
                .monitor(context.getMonitor())
                .retryPolicy(retryPolicy)
                .build();
        var loaderManager = new LoaderManagerImpl(queue, Collections.emptyList(), DEFAULT_BATCH_SIZE, () -> DEFAULT_RETRY_TIMEOUT_MILLIS);


    }

    @Override
    public void start() {
        ServiceExtension.super.start();
    }

    @Override
    public void shutdown() {
        ServiceExtension.super.shutdown();
    }
}
