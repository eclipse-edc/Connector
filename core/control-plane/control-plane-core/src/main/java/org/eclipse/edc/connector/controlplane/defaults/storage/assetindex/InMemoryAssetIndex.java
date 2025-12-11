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

package org.eclipse.edc.connector.controlplane.defaults.storage.assetindex;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

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
    private final CriterionOperatorRegistry criterionOperatorRegistry;
    private final ReentrantReadWriteLock lock;

    public InMemoryAssetIndex(CriterionOperatorRegistry criterionOperatorRegistry) {
        // fair locks guarantee strong consistency since all waiting threads are processed in order of waiting time
        lock = new ReentrantReadWriteLock(true);
        this.criterionOperatorRegistry = criterionOperatorRegistry;
    }

    @Override
    public Stream<Asset> queryAssets(QuerySpec querySpec) {
        lock.readLock().lock();
        try {
            Comparator<Asset> comparator = querySpec.getSortField() == null
                    ? (o1, o2) -> 0
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
            var id = asset.getId();
            if (cache.containsKey(id)) {
                return StoreResult.alreadyExists(format(ASSET_EXISTS_TEMPLATE, id));
            }
            cache.put(asset.getId(), asset);
        } finally {
            lock.writeLock().unlock();
        }
        return StoreResult.success();
    }

    @Override
    public StoreResult<Asset> deleteById(String assetId) {
        lock.writeLock().lock();
        try {
            return Optional.ofNullable(cache.remove(assetId))
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
                cache.put(asset.getId(), asset);
                return StoreResult.success(asset);
            }
            return StoreResult.notFound(format(ASSET_NOT_FOUND_TEMPLATE, id));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public DataAddress resolveForAsset(String assetId) {
        Objects.requireNonNull(assetId, "assetId");
        lock.readLock().lock();
        try {
            return Optional.ofNullable(cache.get(assetId)).map(Asset::getDataAddress).orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    private Stream<Asset> filterBy(List<Criterion> criteria) {
        var predicate = criteria.stream()
                .map(criterionOperatorRegistry::toPredicate)
                .reduce(x -> true, Predicate::and);

        return cache.values().stream()
                .filter(predicate);
    }

}
