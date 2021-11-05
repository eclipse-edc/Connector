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
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public interface AssetStorage {
    @Nullable
    Asset getAsset(@NotNull String assetId);

    @NotNull
    Iterator<Asset> getAssets();

    void add(@NotNull Asset asset);

    /**
     * Returns Assets after a given Asset ID (excluded). The first item in the stream is the Asset nearest to the given Asset ID.
     *
     * @param assetId, excluded
     * @return range of assets
     */
    @NotNull
    Iterator<Asset> getAssetsAscending(@NotNull String assetId);
}
