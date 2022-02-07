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
package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.List;
import java.util.stream.Stream;

/**
 * A stub asset index that contains no assets.
 */
public class NullAssetIndex implements AssetIndex {

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        return Stream.empty();
    }

    @Override
    public Stream<Asset> queryAssets(List<Criterion> criteria) {
        return Stream.empty();
    }

    @Override
    public Asset findById(String assetId) {
        return null;
    }
}
