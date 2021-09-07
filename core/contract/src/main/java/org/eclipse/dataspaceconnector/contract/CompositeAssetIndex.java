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
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.function.Function;
import java.util.stream.Stream;

class CompositeAssetIndex implements AssetIndex {
    private final AssetIndexLocator assetIndexLocator;
    private final Monitor monitor;

    public CompositeAssetIndex(
            final AssetIndexLocator assetIndexLocator,
            final Monitor monitor) {
        this.assetIndexLocator = assetIndexLocator;
        this.monitor = monitor;
    }

    @Override
    public Stream<Asset> queryAssets(final AssetSelectorExpression expression) {
        return assetIndexLocator.getAssetIndexes()
                .stream()
                .flatMap(prepareInvocation(expression));
    }

    private Function<AssetIndex, Stream<Asset>> prepareInvocation(final AssetSelectorExpression assetSelectorExpression) {
        return (assetIndex) -> {
            monitor.debug(String.format("Querying %s", assetIndex.getClass().getName()));
            return assetIndex.queryAssets(assetSelectorExpression);
        };
    }
}
