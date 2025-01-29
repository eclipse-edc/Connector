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
 *       Cofinity-X - expire DidDocuments after some time
 *
 */

package org.eclipse.edc.iam.did.resolution;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.resolution.DidResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.collection.ConcurrentLruCache;
import org.eclipse.edc.util.collection.TimestampedValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation, that delegates to several {@link DidResolver} objects, caching the results in a {@link ConcurrentLruCache}
 */
public class DidResolverRegistryImpl implements DidResolverRegistry {
    public static final String DID_SEPARATOR = ":";
    private static final String DID = "did";
    private static final int DID_PREFIX = 0;
    private static final int DID_METHOD_NAME = 1;
    private final ConcurrentLruCache<String, TimestampedValue<DidDocument>> didCache;
    private final Map<String, DidResolver> resolvers = new HashMap<>();
    private final long cacheValidity; // clean up cache every 5 minutes
    private final Clock clock;


    public DidResolverRegistryImpl(Clock clock, long cacheValidityMillis) {
        this(50, cacheValidityMillis, clock);
    }

    /**
     * Constructs a DidResolverRegistryImpl object with the specified cache size.
     *
     * @param cacheSize     the maximum number of entries that the cache can hold. Pass 0 to effectively deactivate the cache.
     * @param cacheValidity expiry time for cached DID Documents in milliseconds
     * @param clock         the {@link Clock}
     */
    public DidResolverRegistryImpl(int cacheSize, long cacheValidity, Clock clock) {
        didCache = new ConcurrentLruCache<>(cacheSize);
        this.cacheValidity = cacheValidity;
        this.clock = clock;
    }


    @Override
    public void register(DidResolver resolver) {
        resolvers.put(resolver.getMethod(), resolver);
    }

    @Override
    public Result<DidDocument> resolve(String didKey) {
        Objects.requireNonNull(didKey);

        if (!isSupported(didKey)) {
            return Result.failure("This DID is not supported by any of the resolvers: %s".formatted(didKey));
        }

        var resolver = getResolverFor(didKey);
        return resolveCachedDocument(didKey, resolver);
    }

    @Override
    public boolean isSupported(String didKey) {
        var res = getResolverFor(didKey);
        return res != null;
    }

    @Nullable
    private DidResolver getResolverFor(String didKey) {
        var tokens = didKey.split(DID_SEPARATOR);
        if (tokens.length < 3) {
            return null;
        }
        if (!DID.equalsIgnoreCase(tokens[DID_PREFIX])) {
            return null;
        }
        var methodName = tokens[DID_METHOD_NAME];
        return resolvers.get(methodName);
    }

    @NotNull
    private Result<DidDocument> resolveCachedDocument(String didKey, DidResolver resolver) {
        var cacheEntry = didCache.get(didKey);
        DidDocument didDocument = null;

        if (cacheEntry != null) {
            if (cacheEntry.isExpired(clock)) { // lazy evict expired values
                didCache.remove(didKey);
            } else {
                didDocument = cacheEntry.value();
            }
        }
        if (didDocument == null) { //resolve the did document again, put in cache

            var resolveResult = resolver.resolve(didKey);
            if (resolveResult.failed()) {
                return resolveResult;
            }
            didDocument = resolveResult.getContent();
            didCache.put(didKey, new TimestampedValue<>(didDocument, cacheValidity));
        }

        return Result.success(didDocument);
    }
}
