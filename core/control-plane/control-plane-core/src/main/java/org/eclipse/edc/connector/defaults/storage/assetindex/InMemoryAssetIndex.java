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

package org.eclipse.edc.connector.defaults.storage.assetindex;

import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.AssetPredicateConverter;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * An ephemeral asset index, that is also a DataAddressResolver
 */
public class InMemoryAssetIndex implements AssetIndex {
    private final Map<String, Asset> cache = new ConcurrentHashMap<>();
    private final Map<String, DataAddress> dataAddresses = new ConcurrentHashMap<>();
    private final AssetPredicateConverter predicateConverter = new AssetPredicateConverter();
    private final ReentrantReadWriteLock lock;

    public InMemoryAssetIndex() {
        // fair locks guarantee strong consistency since all waiting threads are processed in order of waiting time
        lock = new ReentrantReadWriteLock(true);
    }

    @Override
    public Stream<Asset> queryAssets(QuerySpec querySpec) {
        lock.readLock().lock();
        try {
            var comparator = querySpec.getSortField() == null
                    ? (Comparator<Asset>) (o1, o2) -> 0
                    : new AssetComparator(querySpec.getSortField(), querySpec.getSortOrder());

            return filterBy(querySpec.getFilterExpression())
                    .sorted(comparator)
                    .skip(querySpec.getOffset()).limit(querySpec.getLimit());

        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Asset findById(String assetId) {
        lock.readLock().lock();
        try {
            return cache.values().stream()
                    .filter(asset -> asset.getId().equals(assetId))
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public StoreResult<Void> create(Asset asset) {
        lock.writeLock().lock();
        try {
            if (asset.hasDuplicatePropertyKeys()) {
                var msg = format(DUPLICATE_PROPERTY_KEYS_TEMPLATE);
                return StoreResult.duplicateKeys(msg);
            }

            var id = asset.getId();
            if (cache.containsKey(id)) {
                return StoreResult.alreadyExists(format(ASSET_EXISTS_TEMPLATE, id));
            }
            add(asset, asset.getDataAddress());
        } finally {
            lock.writeLock().unlock();
        }
        return StoreResult.success();
    }

    @Override
    public StoreResult<Asset> deleteById(String assetId) {
        lock.writeLock().lock();
        try {
            return Optional.ofNullable(delete(assetId))
                    .map(StoreResult::success)
                    .orElse(StoreResult.notFound(format(ASSET_NOT_FOUND_TEMPLATE, assetId)));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public long countAssets(List<Criterion> criteria) {
        return filterBy(criteria).count();
    }

    @Override
    public StoreResult<Asset> updateAsset(Asset asset) {
        lock.writeLock().lock();
        try {
            var id = asset.getId();
            Objects.requireNonNull(asset, "asset");
            Objects.requireNonNull(id, "assetId");
            if (cache.containsKey(id)) {
                cache.put(id, asset);
                return StoreResult.success(asset);
            }
            return StoreResult.notFound(format(ASSET_NOT_FOUND_TEMPLATE, id));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public StoreResult<DataAddress> updateDataAddress(String assetId, DataAddress dataAddress) {
        lock.writeLock().lock();
        try {
            Objects.requireNonNull(dataAddress, "dataAddress");
            Objects.requireNonNull(assetId, "asset.getId()");
            if (dataAddresses.containsKey(assetId)) {
                dataAddresses.put(assetId, dataAddress);
                return StoreResult.success(dataAddress);
            }
            return StoreResult.notFound(format(DATA_ADDRESS_NOT_FOUND_TEMPLATE, assetId));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public DataAddress resolveForAsset(String assetId) {
        Objects.requireNonNull(assetId, "assetId");
        lock.readLock().lock();
        try {
            return dataAddresses.get(assetId);
        } finally {
            lock.readLock().unlock();
        }
    }

    private Stream<Asset> filterBy(List<Criterion> criteria) {
        var predicate = criteria.stream()
                .map(predicateConverter::convert)
                .reduce(x -> true, Predicate::and);

        return cache.values().stream()
                .filter(predicate);
    }

    private Asset delete(String assetId) {
        dataAddresses.remove(assetId);
        return cache.remove(assetId);
    }

    /**
     * this method is NOT secured with locks, any guarding must take place in the calling method!
     */
    private void add(Asset asset, DataAddress address) {
        var id = asset.getId();
        Objects.requireNonNull(asset, "asset");
        Objects.requireNonNull(id, "asset.getId()");
        cache.put(id, asset);
        dataAddresses.put(id, address);
    }

    private record AssetComparator(String sortField, SortOrder sortOrder) implements Comparator<Asset> {

        @Override
        public int compare(Asset asset1, Asset asset2) {
            var f1 = asComparable(asset1.getPropertyOrPrivate(sortField));
            var f2 = asComparable(asset2.getPropertyOrPrivate(sortField));

            if (f1 == null || f2 == null) {
                throw new IllegalArgumentException(format("Cannot sort by field %s, it does not exist on one or more Assets", sortField));
            }
            return sortOrder == SortOrder.ASC ? f1.compareTo(f2) : f2.compareTo(f1);
        }

        private @Nullable Comparable<Object> asComparable(Object property) {
            return property instanceof Comparable ? (Comparable<Object>) property : null;
        }
    }
}
