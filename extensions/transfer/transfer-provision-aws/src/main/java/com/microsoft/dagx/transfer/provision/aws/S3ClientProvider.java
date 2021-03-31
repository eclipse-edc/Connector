package com.microsoft.dagx.transfer.provision.aws;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR;

/**
 * Provides reusable S3 clients that are configured to connect to specific regions and endpoints. Clients share a common thread pool.
 *
 * The clients are immutable and created when an instance is built. When finished with the provider, {@link #shutdown()} must be called to release resources.
 */
public class S3ClientProvider implements ClientProvider {
    private static final Set<String> DEFAULT_REGIONS = Set.of(Region.US_EAST_1.id(), Region.EU_CENTRAL_1.id());

    private Map<String, S3AsyncClient> cache;

    private ThreadPoolExecutor executor;

    private AwsCredentialsProvider credentialsProvider;

    public S3AsyncClient clientFor(String key) {
        S3AsyncClient client = cache.get(key);
        if (client == null) {
            throw new IllegalArgumentException("The key is not configured as a supported region or endpoint: " + key);
        }
        return client;
    }

    /**
     * Releases resources used by the provider.
     */
    public void shutdown() {
        if (executor != null) {
            cache.values().forEach(SdkAutoCloseable::close);
            executor.shutdown();
        }
    }

    private S3ClientProvider() {
    }

    public static class Builder {
        private int threadPoolSize = 50; // default thread pool
        private Set<String> regions = DEFAULT_REGIONS;
        private Set<String> endpoints = Collections.emptySet();

        private S3ClientProvider provider;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            provider.credentialsProvider = credentialsProvider;
            return this;
        }

        public Builder regions(Set<String> regions) {
            regions = new HashSet<>(regions);
            return this;
        }

        public Builder endpoints(Set<String> endpoints) {
            endpoints = new HashSet<>(endpoints);
            return this;
        }

        public Builder threadPoolSize(int size) {
            threadPoolSize = size;
            return this;
        }

        public S3ClientProvider build() {
            Objects.requireNonNull(provider.credentialsProvider, "credentialsProvider");
            if (regions.isEmpty() && endpoints.isEmpty()) {
                throw new IllegalStateException("One or more S3 regions or endpoints must be configured");
            }

            LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(10_000);
            ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("aws-async").build();
            provider.executor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 10, TimeUnit.SECONDS, queue, threadFactory);
            provider.executor.allowCoreThreadTimeOut(true);

            var cache = new HashMap<String, S3AsyncClient>();
            regions.forEach(region -> createRegionClient(region, provider.executor, provider.credentialsProvider, cache));
            endpoints.forEach(endpoint -> createEndpointClient(endpoint, provider.executor, provider.credentialsProvider, cache));
            provider.cache = Collections.unmodifiableMap(cache);
            return provider;
        }

        private void createRegionClient(String regionId, ThreadPoolExecutor executor, AwsCredentialsProvider credentialsProvider, Map<String, S3AsyncClient> cache) {
            S3AsyncClientBuilder builder = S3AsyncClient.builder();
            builder.asyncConfiguration(b -> b.advancedOption(FUTURE_COMPLETION_EXECUTOR, executor));
            S3AsyncClient client = builder.region(Region.of(regionId)).credentialsProvider(credentialsProvider).build();
            cache.put(regionId, client);
        }

        private void createEndpointClient(String endpoint, ThreadPoolExecutor executor, AwsCredentialsProvider credentialsProvider, Map<String, S3AsyncClient> cache) {
            S3AsyncClientBuilder builder = S3AsyncClient.builder();
            builder.asyncConfiguration(b -> b.advancedOption(FUTURE_COMPLETION_EXECUTOR, executor));
            S3AsyncClient client = builder.endpointOverride(URI.create(endpoint)).credentialsProvider(credentialsProvider).build();
            cache.put(endpoint, client);
        }

        private Builder() {
            provider = new S3ClientProvider();
        }
    }


}
