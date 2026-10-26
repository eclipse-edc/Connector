/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.catalog;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIdCompatibility;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIdCompatibility.ASSET_ID_FIELD;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.EQUAL;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.IN;

/**
 * Finds the contract definitions whose assets selector matches an asset, for one catalog request.
 * <p>
 * The selector predicates are built once. Definitions that select assets by their ids ({@code id = <asset id>} or
 * {@code id in [<asset ids>]}), the common case, are indexed by those ids, so an asset is only tested against the
 * definitions indexed under its id and the ones that are not indexed, instead of against all of them. The candidates
 * are still tested with their predicates, so the result is the same as testing every definition.
 */
class ContractDefinitionMatcher {

    private final List<ContractDefinition> definitions;
    private final List<Predicate<Asset>> predicates;
    private final Map<String, List<Integer>> indexedByAssetId = new HashMap<>();
    private final List<Integer> notIndexed = new ArrayList<>();

    ContractDefinitionMatcher(List<ContractDefinition> definitions, CriterionOperatorRegistry criterionOperatorRegistry) {
        this.definitions = definitions;
        this.predicates = definitions.stream()
                .map(definition -> definition.getAssetsSelector().stream()
                        .map(criterionOperatorRegistry::<Asset>toPredicate)
                        .reduce(x -> true, Predicate::and))
                .toList();

        for (var i = 0; i < definitions.size(); i++) {
            var assetIds = selectedAssetIds(definitions.get(i));
            if (assetIds == null) {
                notIndexed.add(i);
            } else {
                for (var assetId : assetIds) {
                    indexedByAssetId.computeIfAbsent(assetId, k -> new ArrayList<>()).add(i);
                }
            }
        }
    }

    /**
     * The definitions whose assets selector matches the asset, in the order they were given.
     */
    List<ContractDefinition> matching(Asset asset) {
        var indexed = indexedByAssetId.get(asset.getId());
        if (indexed == null) {
            return matching(asset, notIndexed);
        }
        var candidates = new ArrayList<Integer>(indexed.size() + notIndexed.size());
        candidates.addAll(indexed);
        candidates.addAll(notIndexed);
        candidates.sort(null);
        return matching(asset, candidates);
    }

    private List<ContractDefinition> matching(Asset asset, List<Integer> candidates) {
        var result = new ArrayList<ContractDefinition>();
        for (var i : candidates) {
            if (predicates.get(i).test(asset)) {
                result.add(definitions.get(i));
            }
        }
        return result;
    }

    /**
     * The ids of the assets selected by the definition, if its selector is exactly one {@code id = <asset id>} or
     * {@code id in [<asset ids>]} criterion with string ids, otherwise {@code null}.
     */
    private static Set<String> selectedAssetIds(ContractDefinition definition) {
        var selector = definition.getAssetsSelector();
        if (selector.size() != 1 || !isIdOperand(selector.get(0))) {
            return null;
        }
        var criterion = selector.get(0);
        var operator = criterion.getOperator().toLowerCase();
        if (EQUAL.equals(operator) && criterion.getOperandRight() instanceof String assetId) {
            return Set.of(assetId);
        }
        if (IN.equals(operator) && criterion.getOperandRight() instanceof Iterable<?> values) {
            // a set, so that an id listed twice does not make the definition a candidate twice
            var assetIds = new LinkedHashSet<String>();
            for (var value : values) {
                if (!(value instanceof String assetId)) {
                    return null;
                }
                assetIds.add(assetId);
            }
            return assetIds;
        }
        return null;
    }

    private static boolean isIdOperand(Criterion criterion) {
        return ASSET_ID_FIELD.equals(criterion.getOperandLeft()) || AssetIdCompatibility.isLegacyIdOperand(criterion.getOperandLeft());
    }
}
