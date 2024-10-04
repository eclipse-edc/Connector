/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.controlplane.catalog;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.catalog.spi.ContractDefinitionResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.ResolvedContractDefinitions;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.defaults.storage.assetindex.InMemoryAssetIndex;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.query.asset.AssetPropertyLookup;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.message.Range;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This could be seen as se second part of the {@code DatasetResolverImplTest}, using the in-mem asset index
 */
class DatasetResolverImplIntegrationTest {

    private final ContractDefinitionResolver contractDefinitionResolver = mock();
    private AssetIndex assetIndex;

    private DatasetResolver resolver;

    @BeforeEach
    void setUp() {
        PolicyDefinitionStore policyStore = mock();
        var criterionOperatorRegistry = CriterionOperatorRegistryImpl.ofDefaults();
        criterionOperatorRegistry.registerPropertyLookup(new AssetPropertyLookup());
        assetIndex = new InMemoryAssetIndex(criterionOperatorRegistry);
        resolver = new DatasetResolverImpl(
                contractDefinitionResolver,
                assetIndex,
                policyStore,
                mock(),
                criterionOperatorRegistry);
        var policyDefinition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();
        when(policyStore.findById(any())).thenReturn(policyDefinition);
    }

    @Test
    void shouldLimitResult_withHeterogeneousChunks() {
        var assets1 = range(10, 24).mapToObj(i -> createAsset("asset" + i).build()).collect(Collectors.toList());
        var assets2 = range(24, 113).mapToObj(i -> createAsset("asset" + i).build()).collect(Collectors.toList());
        var assets3 = range(113, 178).mapToObj(i -> createAsset("asset" + i).build()).collect(Collectors.toList());

        assets1.forEach(assetIndex::create);
        assets2.forEach(assetIndex::create);
        assets3.forEach(assetIndex::create);

        var def1 = getContractDefBuilder("def1").assetsSelector(selectorFrom(assets1)).build();
        var def2 = getContractDefBuilder("def2").assetsSelector(selectorFrom(assets2)).build();
        var def3 = getContractDefBuilder("def3").assetsSelector(selectorFrom(assets3)).build();

        when(contractDefinitionResolver.resolveFor(isA(ParticipantAgent.class))).thenReturn(new ResolvedContractDefinitions(List.of(def1, def2, def3)));

        var from = 20;
        var to = 50;
        var querySpec = QuerySpec.Builder.newInstance().range(new Range(from, to)).build();

        var datasets = resolver.query(createAgent(), querySpec);

        assertThat(datasets).hasSize(to - from);
    }

    @ParameterizedTest
    @ArgumentsSource(RangeProvider.class)
    void should_return_offers_subset_when_across_multiple_contract_definitions(int from, int to) {
        var assets1 = range(0, 10).mapToObj(i -> createAsset("asset-" + i).build()).collect(Collectors.toList());
        var assets2 = range(10, 20).mapToObj(i -> createAsset("asset-" + i).build()).collect(Collectors.toList());
        var maximumRange = max(0, (assets1.size() + assets2.size()) - from);
        var requestedRange = to - from;

        assets1.forEach(assetIndex::create);
        assets2.forEach(assetIndex::create);

        var contractDefinition1 = getContractDefBuilder("contract-definition-")
                .assetsSelector(selectorFrom(assets1)).build();
        var contractDefinition2 = getContractDefBuilder("contract-definition-")
                .assetsSelector(selectorFrom(assets2)).build();

        when(contractDefinitionResolver.resolveFor(isA(ParticipantAgent.class))).thenReturn(new ResolvedContractDefinitions(List.of(contractDefinition1, contractDefinition2)));
        var querySpec = QuerySpec.Builder.newInstance().range(new Range(from, to)).build();

        var datasets = resolver.query(createAgent(), querySpec);

        assertThat(datasets).hasSize(min(requestedRange, maximumRange));
    }

    @Test
    void shouldLimitResult_insufficientAssets() {
        var assets1 = range(0, 12).mapToObj(i -> createAsset("asset" + i).build()).toList();
        var assets2 = range(12, 18).mapToObj(i -> createAsset("asset" + i).build()).toList();

        assets1.forEach(assetIndex::create);
        assets2.forEach(assetIndex::create);

        var def1 = getContractDefBuilder("def1").assetsSelector(selectorFrom(assets1)).build();
        var def2 = getContractDefBuilder("def2").assetsSelector(selectorFrom(assets2)).build();

        when(contractDefinitionResolver.resolveFor(isA(ParticipantAgent.class))).thenReturn(new ResolvedContractDefinitions(List.of(def1, def2)));

        var from = 14;
        var to = 50;
        var querySpec = QuerySpec.Builder.newInstance().range(new Range(from, to)).build();

        // 4 definitions, 10 assets each = 40 offers total -> offset 20 ==> result = 20
        var dataset = resolver.query(createAgent(), querySpec);

        assertThat(dataset).hasSize(4);
    }

    @Test
    void shouldLimitResult_pageOffsetLargerThanNumAssets() {
        var contractDefinition = range(0, 2).mapToObj(i -> getContractDefBuilder(String.valueOf(i)).build());
        when(contractDefinitionResolver.resolveFor(isA(ParticipantAgent.class))).thenReturn(new ResolvedContractDefinitions(contractDefinition.toList()));

        var from = 25;
        var to = 50;
        var querySpec = QuerySpec.Builder.newInstance().range(new Range(from, to)).build();
        // 2 definitions, 10 assets each = 20 offers total -> offset of 25 is outside
        var datasets = resolver.query(createAgent(), querySpec);

        assertThat(datasets).isEmpty();
    }

    @NotNull
    private ParticipantAgent createAgent() {
        return new ParticipantAgent(emptyMap(), emptyMap());
    }

    private List<Criterion> selectorFrom(Collection<Asset> assets1) {
        var ids = assets1.stream().map(Asset::getId).toList();
        return List.of(new Criterion(Asset.PROPERTY_ID, "in", ids));
    }

    private ContractDefinition.Builder getContractDefBuilder(String id) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .assetsSelector(emptyList());
    }

    private Asset.Builder createAsset(String id) {
        return Asset.Builder.newInstance()
                .id(id)
                .name("test asset " + id)
                .dataAddress(DataAddress.Builder.newInstance().type("test-type").build());
    }

    static class RangeProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(0, 12),
                    Arguments.of(8, 14),
                    Arguments.of(0, 999),
                    Arguments.of(4, 888),
                    Arguments.of(3, 20),
                    Arguments.of(23, 25)
            );
        }
    }
}
