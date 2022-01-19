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

package org.eclipse.dataspaceconnector.aws.s3.core;

import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.sts.StsAsyncClient;
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

import static org.eclipse.dataspaceconnector.aws.s3.core.SdkClientBuilders.buildIamClient;
import static org.eclipse.dataspaceconnector.aws.s3.core.SdkClientBuilders.buildS3Client;
import static org.eclipse.dataspaceconnector.aws.s3.core.SdkClientBuilders.buildStsClient;


/**
 * Provides reusable SDK clients that are configured to connect to specific regions and endpoints. Clients share a common thread pool.
 * The clients are immutable and created when an instance is built. When finished with the provider, {@link #shutdown()} must be called to release resources.
 */
public class SdkClientProvider implements ClientProvider {
    private static final Set<String> DEFAULT_REGIONS = Set.of(Region.US_EAST_1.id(), Region.EU_CENTRAL_1.id());

    private Map<String, S3AsyncClient> s3Cache;
    private Map<String, StsAsyncClient> stsCache;
    private IamAsyncClient iamClient;

    private ThreadPoolExecutor executor;

    private AwsCredentialsProvider credentialsProvider;

    private SdkClientProvider() {
    }

    @Override
    public <T extends SdkClient> T clientFor(Class<T> type, String key) {
        if (type.isAssignableFrom(S3AsyncClient.class)) {
            S3AsyncClient client = s3Cache.get(key);
            return checkAndReturn(type, key, client);
        } else if (type.isAssignableFrom(IamAsyncClient.class)) {
            return checkAndReturn(type, key, iamClient);
        } else if (type.isAssignableFrom(StsAsyncClient.class)) {
            StsAsyncClient client = stsCache.get(key);
            return type.cast(client);
        }
        throw new IllegalArgumentException("Unsupported SDK type: " + type.getName());
    }

    /**
     * Releases resources used by the provider.
     */
    public void shutdown() {
        if (executor != null) {
            s3Cache.values().forEach(SdkAutoCloseable::close);
            iamClient.close();
            stsCache.values().forEach(SdkAutoCloseable::close);
            executor.shutdown();
        }
    }

    @NotNull
    private <T extends SdkClient> T checkAndReturn(Class<T> type, String key, SdkClient client) {
        if (client == null) {
            throw new IllegalArgumentException("The key is not configured as a supported region or endpoint: " + key);
        }
        return type.cast(client);
    }

    public static class Builder {
        private int threadPoolSize = 50; // default thread pool
        private Set<String> regions = DEFAULT_REGIONS;
        private Set<String> endpoints = Collections.emptySet();

        private final SdkClientProvider provider;

        private Builder() {
            provider = new SdkClientProvider();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            provider.credentialsProvider = credentialsProvider;
            return this;
        }

        public Builder regions(Set<String> regions) {
            this.regions = new HashSet<>(regions);
            return this;
        }

        public Builder endpoints(Set<String> endpoints) {
            this.endpoints = new HashSet<>(endpoints);
            return this;
        }

        public Builder threadPoolSize(int size) {
            threadPoolSize = size;
            return this;
        }

        public SdkClientProvider build() {
            Objects.requireNonNull(provider.credentialsProvider, "credentialsProvider");
            if (regions.isEmpty() && endpoints.isEmpty()) {
                throw new IllegalStateException("One or more S3 regions or endpoints must be configured");
            }

            LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(10_000);
            ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("aws-async").build();
            provider.executor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 10, TimeUnit.SECONDS, queue, threadFactory);
            provider.executor.allowCoreThreadTimeOut(true);

            initS3Cache();
            initIamClient();
            initStsCache();

            return provider;
        }

        private void initS3Cache() {
            var cache = new HashMap<String, S3AsyncClient>();
            regions.forEach(region -> cache.put(region, buildS3Client(b -> b.region(Region.of(region)), provider.executor, provider.credentialsProvider)));
            endpoints.forEach(endpoint -> cache.put(endpoint, buildS3Client(b -> b.endpointOverride(URI.create(endpoint)), provider.executor, provider.credentialsProvider)));
            provider.s3Cache = Collections.unmodifiableMap(cache);
        }

        private void initIamClient() {
            provider.iamClient = buildIamClient(b -> b.region(Region.AWS_GLOBAL), provider.executor, provider.credentialsProvider);
        }

        private void initStsCache() {
            var cache = new HashMap<String, StsAsyncClient>();
            regions.forEach(region -> cache.put(region, buildStsClient(b -> b.region(Region.of(region)), provider.executor, provider.credentialsProvider)));
            endpoints.forEach(endpoint -> cache.put(endpoint, buildStsClient(b -> b.endpointOverride(URI.create(endpoint)), provider.executor, provider.credentialsProvider)));
            provider.stsCache = Collections.unmodifiableMap(cache);
        }
    }


}
