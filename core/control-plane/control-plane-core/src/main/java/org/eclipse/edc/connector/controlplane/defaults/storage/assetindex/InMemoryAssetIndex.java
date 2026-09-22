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
import org.eclipse.edc.connector.controlplane.defaults.storage.ParticipantResourceKey;
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
 * An ephemeral asset index, that is also a DataAddressResolver. Assets are keyed by participant context id and asset id.
 */
public class InMemoryAssetIndex implements AssetIndex {
    private final Map<ParticipantResourceKey, Asset> cache = new ConcurrentHashMap<>();
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
    public Asset findById(String participantContextId, String assetId) {
        var key = ParticipantResourceKey.of(participantContextId, assetId);
        lock.readLock().lock();
        try {
            return cache.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public StoreResult<Void> create(Asset asset) {
        var key = ParticipantResourceKey.of(asset.getParticipantContextId(), asset.getId());
        lock.writeLock().lock();
        try {
            if (cache.containsKey(key)) {
                return StoreResult.alreadyExists(format(ASSET_EXISTS_TEMPLATE, asset.getId()));
            }
            cache.put(key, asset);
        } finally {
            lock.writeLock().unlock();
        }
        return StoreResult.success();
    }

    @Override
    public StoreResult<Asset> deleteById(String participantContextId, String assetId) {
        var key = ParticipantResourceKey.of(participantContextId, assetId);
        lock.writeLock().lock();
        try {
            return Optional.ofNullable(cache.remove(key))
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
        Objects.requireNonNull(asset, "asset");
        var key = ParticipantResourceKey.of(asset.getParticipantContextId(), asset.getId());
        lock.writeLock().lock();
        try {
            if (cache.containsKey(key)) {
                cache.put(key, asset);
                return StoreResult.success(asset);
            }
            return StoreResult.notFound(format(ASSET_NOT_FOUND_TEMPLATE, asset.getId()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public DataAddress resolveForAsset(String participantContextId, String assetId) {
        return Optional.ofNullable(findById(participantContextId, assetId)).map(Asset::getDataAddress).orElse(null);
    }

    private Stream<Asset> filterBy(List<Criterion> criteria) {
        var predicate = criteria.stream()
                .map(criterionOperatorRegistry::toPredicate)
                .reduce(x -> true, Predicate::and);

        return cache.values().stream()
                .filter(predicate);
    }

}
