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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An ephemeral asset index.
 */
public class InMemoryAssetIndex implements AssetIndex {
    private final List<Asset> cache = new LinkedList<>();
    private final Map<String, DataAddress> dataAddresses = new HashMap<>();

    public InMemoryAssetIndex(Monitor monitor) {
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        Objects.requireNonNull(expression, "expression");
        return filterByPredicate(cache, collatePredicateWithAnd(expression));

    }

    @Override
    public Asset findById(String assetId) {
        Predicate<Asset> predicate = (asset) -> asset.getId().equals(assetId);
        var assets = filterByPredicate(cache, predicate).collect(Collectors.toList());

        if (assets.size() > 1) {
            throw new IllegalStateException("findById() was expected to return 1 result but returned " + assets.size());
        }

        return assets.isEmpty() ? null : assets.get(0);
    }

    @Override
    public DataAddress resolveForAsset(Asset asset) {
        Objects.requireNonNull(asset, "asset");
        if (!dataAddresses.containsKey(asset.getId()) || dataAddresses.get(asset.getId()) == null) {
            throw new IllegalArgumentException("No DataAddress found for Asset ID=" + asset.getId());
        }
        return dataAddresses.get(asset.getId());

    }

    void add(Asset asset, DataAddress address) {
        Objects.requireNonNull(asset, "asset");
        Objects.requireNonNull(asset.getId(), "asset.getId()");
        cache.add(asset);

        if (address != null) {
            dataAddresses.put(asset.getId(), address);
        }
    }

    private Predicate<Asset> collatePredicateWithAnd(AssetSelectorExpression expression) {
        return expression.getFilters().stream().reduce(x -> true, Predicate::and);
    }

    private Stream<Asset> filterByPredicate(List<Asset> assets, Predicate<Asset> predicate) {
        return assets.stream().filter(predicate);
    }

}
