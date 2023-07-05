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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.contract.offer;

import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionResolver;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferQuery;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferResolver;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.defaults.storage.assetindex.InMemoryAssetIndex;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.message.Range;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This could be seen as se second part of the {@code ContractOfferServiceImplTest}, using the in-mem asset index
 */
class ContractOfferResolverImplIntegrationTest {
    private static final String PARTICIPANT_ID = "urn:connector:provider";

    private final Instant now = Instant.now();
    private final Clock clock = Clock.fixed(now, UTC);
    private final ContractDefinitionResolver contractDefinitionResolver = mock(ContractDefinitionResolver.class);
    private final ParticipantAgentService agentService = mock(ParticipantAgentService.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final Monitor monitor = mock(Monitor.class);
    private AssetIndex assetIndex;
    private ContractOfferResolver contractOfferResolver;

    @BeforeEach
    void setUp() {
        assetIndex = new InMemoryAssetIndex();
        contractOfferResolver = new ContractOfferResolverImpl(PARTICIPANT_ID, agentService, contractDefinitionResolver, assetIndex, policyStore, clock, monitor);
    }

    @Test
    void shouldLimitResult_withHeterogenousChunks() {
        var assets1 = range(10, 24).mapToObj(i -> createAsset("asset" + i).build()).collect(Collectors.toList());
        var assets2 = range(24, 113).mapToObj(i -> createAsset("asset" + i).build()).collect(Collectors.toList());
        var assets3 = range(113, 178).mapToObj(i -> createAsset("asset" + i).build()).collect(Collectors.toList());

        assets1.forEach(assetIndex::create);
        assets2.forEach(assetIndex::create);
        assets3.forEach(assetIndex::create);

        var def1 = getContractDefBuilder("def1").assetsSelector(selectorFrom(assets1)).build();
        var def2 = getContractDefBuilder("def2").assetsSelector(selectorFrom(assets2)).build();
        var def3 = getContractDefBuilder("def3").assetsSelector(selectorFrom(assets3)).build();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionResolver.definitionsFor(isA(ParticipantAgent.class))).thenAnswer(i -> Stream.of(def1, def2, def3));

        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());


        var from = 20;
        var to = 50;
        var query = ContractOfferQuery.builder().range(new Range(from, to)).claimToken(ClaimToken.Builder.newInstance().build()).build();

        assertThat(contractOfferResolver.queryContractOffers(query)).hasSize(to - from);
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionResolver, times(1)).definitionsFor(isA(ParticipantAgent.class));
        verify(policyStore).findById("contract");
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

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionResolver.definitionsFor(isA(ParticipantAgent.class))).thenAnswer(i -> Stream.of(contractDefinition1, contractDefinition2));
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());

        var query = ContractOfferQuery.builder().range(new Range(from, to)).claimToken(ClaimToken.Builder.newInstance().build()).build();
        assertThat(contractOfferResolver.queryContractOffers(query)).hasSize(min(requestedRange, maximumRange));
    }

    @Test
    void shouldLimitResult_insufficientAssets() {
        var assets1 = range(0, 12).mapToObj(i -> createAsset("asset" + i).build()).collect(Collectors.toList());
        var assets2 = range(12, 18).mapToObj(i -> createAsset("asset" + i).build()).collect(Collectors.toList());

        assets1.forEach(assetIndex::create);
        assets2.forEach(assetIndex::create);

        var def1 = getContractDefBuilder("def1").assetsSelector(selectorFrom(assets1)).build();
        var def2 = getContractDefBuilder("def2").assetsSelector(selectorFrom(assets2)).build();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionResolver.definitionsFor(isA(ParticipantAgent.class))).thenAnswer(i -> Stream.of(def1, def2));
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());

        var from = 14;
        var to = 50;
        var query = ContractOfferQuery.builder().range(new Range(from, to)).claimToken(ClaimToken.Builder.newInstance().build()).build();

        // 4 definitions, 10 assets each = 40 offers total -> offset 20 ==> result = 20
        assertThat(contractOfferResolver.queryContractOffers(query)).hasSize(4);
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionResolver).definitionsFor(isA(ParticipantAgent.class));
        verify(policyStore, atLeastOnce()).findById("contract");
    }

    @Test
    void shouldLimitResult_pageOffsetLargerThanNumAssets() {
        var contractDefinition = range(0, 2).mapToObj(i -> getContractDefBuilder(String.valueOf(i))
                .build());

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionResolver.definitionsFor(isA(ParticipantAgent.class))).thenAnswer(i -> contractDefinition);

        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());

        var from = 25;
        var to = 50;
        var query = ContractOfferQuery.builder().range(new Range(from, to)).claimToken(ClaimToken.Builder.newInstance().build()).build();

        // 2 definitions, 10 assets each = 20 offers total -> offset of 25 is outside
        assertThat(contractOfferResolver.queryContractOffers(query)).isEmpty();
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionResolver).definitionsFor(isA(ParticipantAgent.class));
        verify(policyStore, never()).findById("contract");
    }

    private List<Criterion> selectorFrom(Collection<Asset> assets1) {
        var ids = assets1.stream().map(Asset::getId).toList();
        return List.of(new Criterion(Asset.PROPERTY_ID, "in", ids));
    }

    private ContractDefinition.Builder getContractDefBuilder(String id) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicyId("access")
                .contractPolicyId("contract");
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
