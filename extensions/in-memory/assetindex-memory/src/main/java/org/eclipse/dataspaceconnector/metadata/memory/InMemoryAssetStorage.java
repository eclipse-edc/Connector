/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;

class InMemoryAssetStorage implements AssetStorage {
    private final ConcurrentSkipListSet<Asset> cache;

    public InMemoryAssetStorage() {
        cache = new ConcurrentSkipListSet<>(Comparator.comparing(Asset::getId));
    }

    @Override
    public Asset getAsset(@NotNull String assetId) {
        return cache.tailSet(createComparisonAsset(assetId), true).pollFirst();
    }

    @Override
    @NotNull
    public Iterator<Asset> getAssets() {
        return cache.iterator();
    }

    @Override
    @NotNull
    public Iterator<Asset> getAssetsAscending(@NotNull String assetId) {
        return cache.tailSet(createComparisonAsset(assetId), false).iterator();
    }

    public void add(@NotNull Asset asset) {
        cache.add(Objects.requireNonNull(asset));
    }

    private Asset createComparisonAsset(@NotNull String assetId) {
        Objects.requireNonNull(assetId);
        return Asset.Builder.newInstance().id(assetId).build();
    }

}
