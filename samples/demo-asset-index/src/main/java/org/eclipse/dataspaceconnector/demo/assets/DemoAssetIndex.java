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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DemoAssetIndex implements AssetIndex {

    private static final List<Asset> ASSETS = Arrays.stream(DemoFixtures.FIXTURES)
            .map(DemoFixtures.AssetFactory::create)
            .collect(Collectors.toList());

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        return Optional.ofNullable(expression)
                .map(this::buildPredicate)
                .map(this::filterAssets)
                .orElseGet(Stream::empty);
    }

    @Override
    public Asset findById(String assetId) {
        return ASSETS.stream().filter(a -> a.getId().equals(assetId)).findFirst().orElse(null);
    }

    @Override
    public DataAddress resolveForAsset(Asset asset) {
        return null;
    }

    private Stream<Asset> filterAssets(Predicate<Asset> assetPredicate) {
        return ASSETS.stream().filter(assetPredicate);
    }

    private Predicate<Asset> buildPredicate(AssetSelectorExpression assetSelectorExpression) {
        return buildPredicate(assetSelectorExpression.getFilters());
    }

    private Predicate<Asset> buildPredicate(List<Predicate<Asset>> predicates) {
        // bag for collecting all composable predicates

        // if example asset does not provide any meaningful properties this will
        // lead to true
        return predicates.stream().reduce(x -> true, Predicate::and);
    }
}
