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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DemoAssetIndex implements AssetIndex {

    private static final List<Asset> ASSETS = Arrays.stream(DemoFixtures.FIXTURES)
            .map(DemoFixtures.AssetFactory::create)
            .collect(Collectors.toList());

    @Override
    public Stream<Asset> queryAssets(final AssetSelectorExpression expression) {
        return Optional.ofNullable(expression)
                .map(this::buildPredicate)
                .map(this::filterAssets)
                .orElseGet(Stream::empty);
    }

    private Stream<Asset> filterAssets(final Predicate<Asset> assetPredicate) {
        return ASSETS.stream().filter(assetPredicate);
    }

    private Predicate<Asset> buildPredicate(final AssetSelectorExpression assetSelectorExpression) {
        return buildPredicate(assetSelectorExpression.getFilterLabels());
    }

    private Predicate<Asset> buildPredicate(final Map<String, String> labels) {
        // bag for collecting all composable predicates
        final List<Predicate<Asset>> predicates = new LinkedList<>();

        predicates.add(new LabelsPredicate(Optional.ofNullable(labels).orElseGet(Collections::emptyMap)));

        // if example asset does not provide any meaningful properties this will
        // lead to true
        return predicates.stream().reduce(x -> true, Predicate::and);
    }

    private static class LabelsPredicate implements Predicate<Asset> {
        private final Map<String, String> labels;

        private LabelsPredicate(final Map<String, String> labels) {
            this.labels = labels;
        }

        @Override
        public boolean test(final Asset asset) {
            // iterate through all labels and check for equality
            // Note: map#equals not usable here!
            labels.entrySet().stream().allMatch(kv -> asset.getLabels().containsKey(kv.getKey()) &&
                    kv.getValue().equals(asset.getLabels().get(kv.getKey())));

            return true;
        }
    }
}
