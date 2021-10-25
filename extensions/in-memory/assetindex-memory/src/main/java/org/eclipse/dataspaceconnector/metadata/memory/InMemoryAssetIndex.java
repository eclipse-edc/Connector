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

package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndexLoader;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An ephemeral asset index, that is also a DataAddressResolver and an AssetIndexLoader
 */
public class InMemoryAssetIndex implements AssetIndex, DataAddressResolver, AssetIndexLoader {
    private final Map<String, Asset> cache = new ConcurrentHashMap<>();
    private final Map<String, DataAddress> dataAddresses = new ConcurrentHashMap<>();
    private final CriterionToPredicateConverter predicateFactory;
    private final ReentrantLock lock;

    public InMemoryAssetIndex(Monitor monitor, CriterionToPredicateConverter predicateFactory) {
        this.predicateFactory = predicateFactory;
        //fair locks guarantee strong consistency since all waiting threads are processed in order of waiting time
        lock = new ReentrantLock(true);
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        Objects.requireNonNull(expression, "AssetSelectorExpression can not be null!");
        // do not return anything if expression is empty
        if (expression.getCriteria().isEmpty()) {
            return Stream.empty();
        }

        // select everything ONLY if the special constant is used
        if (expression == AssetSelectorExpression.SELECT_ALL) {
            lock.lock();
            try {
                return cache.values().stream();
            } finally {
                lock.unlock();
            }
        }

        // convert all the criteria into predicates since we're in memory anyway, collate all predicates into one and
        // apply it to the stream
        var rootPredicate = expression.getCriteria().stream().map(predicateFactory::convert).reduce(x -> true, Predicate::and);

        lock.lock();
        try {
            return filterByPredicate(cache, rootPredicate);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Asset findById(String assetId) {
        Predicate<Asset> predicate = (asset) -> asset.getId().equals(assetId);
        List<Asset> assets;
        lock.lock();
        try {
            assets = filterByPredicate(cache, predicate).collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
        return assets.isEmpty() ? null : assets.get(0);
    }

    @Override
    public void insert(Asset asset, DataAddress address) {
        lock.lock();
        try {
            add(asset, address);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void insertAll(Map<Asset, DataAddress> entries) {
        lock.lock();
        try {
            entries.forEach(this::add);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public DataAddress resolveForAsset(String assetId) {
        Objects.requireNonNull(assetId, "assetId");
        lock.lock();
        try {
            if (!dataAddresses.containsKey(assetId) || dataAddresses.get(assetId) == null) {
                throw new IllegalArgumentException("No DataAddress found for Asset ID=" + assetId);
            }
            return dataAddresses.get(assetId);
        } finally {
            lock.unlock();
        }
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
