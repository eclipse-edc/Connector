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
 *
 */

package org.eclipse.edc.iam.did.resolution;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.resolution.DidResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.collection.ConcurrentLruCache;
import org.jetbrains.annotations.NotNull;

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
    private final ConcurrentLruCache<String, DidDocument> didCache;
    private final Map<String, DidResolver> resolvers = new HashMap<>();

    public DidResolverRegistryImpl() {
        didCache = new ConcurrentLruCache<>(50);
    }

    /**
     * Constructs a DidResolverRegistryImpl object with the specified cache size.
     *
     * @param cacheSize the maximum number of entries that the cache can hold. Pass 0 to effectively deactivate the cache.
     */
    public DidResolverRegistryImpl(int cacheSize) {
        didCache = new ConcurrentLruCache<>(cacheSize);
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
        // no need to validate the token length here
        var tokens = didKey.split(DID_SEPARATOR);
        var methodName = tokens[DID_METHOD_NAME];

        var resolver = resolvers.get(methodName);
        if (resolver == null) {
            return Result.failure("No resolver registered for DID Method: " + methodName);
        }
        return resolveCachedDocument(didKey, resolver);
    }

    @Override
    public boolean isSupported(String didKey) {
        var tokens = didKey.split(DID_SEPARATOR);
        if (tokens.length < 3) {
            return false;
        }
        if (!DID.equalsIgnoreCase(tokens[DID_PREFIX])) {
            return false;
        }
        var methodName = tokens[DID_METHOD_NAME];
        return resolvers.containsKey(methodName);
    }

    @NotNull
    private Result<DidDocument> resolveCachedDocument(String didKey, DidResolver resolver) {
        var didDocument = didCache.get(didKey);
        if (didDocument == null) {

            var resolveResult = resolver.resolve(didKey);
            if (resolveResult.failed()) {
                return resolveResult;
            }
            didDocument = resolveResult.getContent();
            didCache.put(didKey, didDocument);
        }

        return Result.success(didDocument);
    }
}
