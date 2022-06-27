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

package org.eclipse.dataspaceconnector.core.defaults.assetindex;

import org.eclipse.dataspaceconnector.common.collection.CollectionUtil;
import org.eclipse.dataspaceconnector.dataloading.AssetEntry;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * An ephemeral asset index, that is also a DataAddressResolver and an AssetLoader
 */
public class InMemoryAssetIndex implements AssetIndex, DataAddressResolver, AssetLoader {
    private final Map<String, Asset> cache = new ConcurrentHashMap<>();
    private final Map<String, DataAddress> dataAddresses = new ConcurrentHashMap<>();
    private final AssetPredicateConverter predicateFactory;
    private final ReentrantReadWriteLock lock;

    public InMemoryAssetIndex() {
        predicateFactory = new AssetPredicateConverter();
        // fair locks guarantee strong consistency since all waiting threads are processed in order of waiting time
        lock = new ReentrantReadWriteLock(true);
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        Objects.requireNonNull(expression, "AssetSelectorExpression can not be null!");

        // select everything ONLY if the special constant is used
        if (expression == AssetSelectorExpression.SELECT_ALL) {
            return queryAssets(QuerySpec.none());
        }

        return queryAssets(QuerySpec.Builder.newInstance().filter(expression.getCriteria()).build());
    }

    @Override
    public Stream<Asset> queryAssets(QuerySpec querySpec) {
        // first filter...
        var expr = querySpec.getFilterExpression();
        Stream<Asset> result;

        lock.readLock().lock();
        try {
            if (CollectionUtil.isNotEmpty(expr)) {
                // convert all the criteria into predicates since we're in memory anyway, collate all predicates into one and
                // apply it to the stream
                var rootPredicate = expr.stream().map(predicateFactory::convert).reduce(x -> true, Predicate::and);
                result = filterByPredicate(cache, rootPredicate);
            } else {
                result = cache.values().stream();
            }

            // ... then sort
            var sortField = querySpec.getSortField();
            if (sortField != null) {
                result = result.sorted((asset1, asset2) -> {
                    var f1 = asComparable(asset1.getProperty(sortField));
                    var f2 = asComparable(asset2.getProperty(sortField));
                    if (f1 == null || f2 == null) {
                        throw new IllegalArgumentException(format("Cannot sort by field %s, it does not exist on one or more Assets", sortField));
                    }
                    return querySpec.getSortOrder() == SortOrder.ASC ? f1.compareTo(f2) : f2.compareTo(f1);
                });
            }

            // ... then limit
            return result.skip(querySpec.getOffset()).limit(querySpec.getLimit());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Asset findById(String assetId) {
        Predicate<Asset> predicate = (asset) -> asset.getId().equals(assetId);
        List<Asset> assets;
        lock.readLock().lock();
        try {
            assets = filterByPredicate(cache, predicate).collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
        return assets.isEmpty() ? null : assets.get(0);
    }

    @Override
    public DataAddress resolveForAsset(String assetId) {
        Objects.requireNonNull(assetId, "assetId");
        lock.readLock().lock();
        try {
            if (!dataAddresses.containsKey(assetId) || dataAddresses.get(assetId) == null) {
                throw new IllegalArgumentException("No DataAddress found for Asset ID=" + assetId);
            }
            return dataAddresses.get(assetId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, Asset> getAssets() {
        return Collections.unmodifiableMap(cache);
    }

    public Map<String, DataAddress> getDataAddresses() {
        return Collections.unmodifiableMap(dataAddresses);
    }

    @Override
    public void accept(AssetEntry item) {
        lock.writeLock().lock();
        try {
            add(item.getAsset(), item.getDataAddress());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Asset deleteById(String assetId) {
        lock.writeLock().lock();
        try {
            return delete(assetId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private @Nullable Comparable asComparable(Object property) {
        return property instanceof Comparable ? (Comparable) property : null;
    }

    private Asset delete(String assetId) {
        dataAddresses.remove(assetId);
        return cache.remove(assetId);
    }

    /**
     * this method is NOT secured with locks, any guarding must take place in the calling method!
     */
    private void add(Asset asset, DataAddress address) {
        String id = asset.getId();
        Objects.requireNonNull(asset, "asset");
        Objects.requireNonNull(id, "asset.getId()");
        cache.put(id, asset);
        dataAddresses.put(id, address);
    }

    private Stream<Asset> filterByPredicate(Map<String, Asset> assets, Predicate<Asset> predicate) {
        return assets.values().stream().filter(predicate);
    }
}
