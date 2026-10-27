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
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.query.asset.AssetPropertyLookup;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;

class ContractDefinitionMatcherTest {

    private final CriterionOperatorRegistry criterionOperatorRegistry = registry();

    @Test
    void matching_shouldReturnDefinitionsSelectingTheAssetById() {
        var definitions = List.of(
                definition("cd-1", new Criterion("id", "=", "asset-1")),
                definition("cd-2", new Criterion(EDC_NAMESPACE + "id", "=", "asset-2")),
                definition("cd-3", new Criterion("id", "=", "asset-1")));

        var matcher = new ContractDefinitionMatcher(definitions, criterionOperatorRegistry);

        assertThat(matcher.matching(asset("asset-1"))).extracting(ContractDefinition::getId).containsExactly("cd-1", "cd-3");
        assertThat(matcher.matching(asset("asset-2"))).extracting(ContractDefinition::getId).containsExactly("cd-2");
        assertThat(matcher.matching(asset("asset-3"))).isEmpty();
    }

    @Test
    void matching_shouldReturnDefinitionsSelectingTheAssetByIdIn() {
        var definitions = List.of(
                definition("cd-1", new Criterion("id", "in", List.of("asset-1", "asset-2"))),
                definition("cd-2", new Criterion(EDC_NAMESPACE + "id", "IN", List.of("asset-2", "asset-3"))));

        var matcher = new ContractDefinitionMatcher(definitions, criterionOperatorRegistry);

        assertThat(matcher.matching(asset("asset-1"))).extracting(ContractDefinition::getId).containsExactly("cd-1");
        assertThat(matcher.matching(asset("asset-2"))).extracting(ContractDefinition::getId).containsExactly("cd-1", "cd-2");
        assertThat(matcher.matching(asset("asset-4"))).isEmpty();
    }

    @Test
    void matching_shouldReturnDefinitionOnce_whenIdListedTwice() {
        var definitions = List.of(definition("cd", new Criterion("id", "in", List.of("asset-1", "asset-1"))));

        var matcher = new ContractDefinitionMatcher(definitions, criterionOperatorRegistry);

        assertThat(matcher.matching(asset("asset-1"))).extracting(ContractDefinition::getId).containsExactly("cd");
    }

    @Test
    void matching_shouldTestDefinitionsNotSelectingById_againstEveryAsset() {
        var definitions = List.of(
                definition("all"),
                definition("by-property", new Criterion("type", "=", "dataset")),
                definition("by-id-and-property", new Criterion("id", "=", "asset-1"), new Criterion("type", "=", "other")),
                definition("by-id-not-in", new Criterion("id", "not in", List.of("asset-1"))),
                definition("by-id-in-with-number", new Criterion("id", "in", List.of("asset-1", 3))));

        var matcher = new ContractDefinitionMatcher(definitions, criterionOperatorRegistry);

        assertThat(matcher.matching(asset("asset-1"))).extracting(ContractDefinition::getId).containsExactly("all", "by-property", "by-id-in-with-number");
        assertThat(matcher.matching(asset("asset-3"))).extracting(ContractDefinition::getId).containsExactly("all", "by-property", "by-id-not-in");
    }

    @Test
    void matching_shouldKeepTheOrderOfTheDefinitions() {
        var definitions = List.of(
                definition("all"),
                definition("by-id", new Criterion("id", "=", "asset-1")),
                definition("by-property", new Criterion("type", "=", "dataset")));

        var matcher = new ContractDefinitionMatcher(definitions, criterionOperatorRegistry);

        assertThat(matcher.matching(asset("asset-1"))).extracting(ContractDefinition::getId).containsExactly("all", "by-id", "by-property");
    }

    private static CriterionOperatorRegistry registry() {
        var registry = CriterionOperatorRegistryImpl.ofDefaults();
        registry.registerPropertyLookup(new AssetPropertyLookup(mock()));
        return registry;
    }

    private static ContractDefinition definition(String id, Criterion... selector) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .participantContextId("participantContextId")
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .assetsSelector(List.of(selector))
                .build();
    }

    private static Asset asset(String id) {
        return Asset.Builder.newInstance().id(id).property("type", "dataset").build();
    }
}
