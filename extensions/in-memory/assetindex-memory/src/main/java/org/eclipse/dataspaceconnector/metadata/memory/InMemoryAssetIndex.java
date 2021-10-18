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
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An ephemeral asset index.
 */
public class InMemoryAssetIndex implements AssetIndex {
    private final Map<String, Asset> cache = new ConcurrentHashMap<>();
    private final CriterionToPredicateConverter predicateFactory;

    public InMemoryAssetIndex(Monitor monitor, CriterionToPredicateConverter predicateFactory) {
        this.predicateFactory = predicateFactory;
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
            return cache.values().stream();
        }

        // convert all the criteria into predicates since we're in memory anyway, collate all predicates into one and
        // apply it to the stream
        var rootPredicate = expression.getCriteria().stream().map(predicateFactory::convert).reduce(x -> true, Predicate::and);

        return filterByPredicate(cache, rootPredicate);

    }

    @Override
    public Asset findById(String assetId) {
        Predicate<Asset> predicate = (asset) -> asset.getId().equals(assetId);
        List<Asset> assets;
        assets = filterByPredicate(cache, predicate).collect(Collectors.toList());
        return assets.isEmpty() ? null : assets.get(0);
    }

    // TODO: address is not used and it should be deleted
    public void add(Asset asset, DataAddress address) {
        Objects.requireNonNull(asset, "asset");
        Objects.requireNonNull(asset.getId(), "asset.getId()");
        cache.put(asset.getId(), asset);
    }

    private Stream<Asset> filterByPredicate(Map<String, Asset> assets, Predicate<Asset> predicate) {
        return assets.values().stream().filter(predicate);
    }

}
