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

package org.eclipse.dataspaceconnector.demo.assets;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.util.List;
import java.util.stream.Stream;

public class DemoAssetIndex implements AssetIndex {

    private static final List<Asset> ASSETS = DemoFixtures.getAssets();

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public Asset findById(String assetId) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public DataAddress resolveForAsset(String assetId) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

}
