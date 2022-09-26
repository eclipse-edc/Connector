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

package org.eclipse.dataspaceconnector.contract.offer;

import org.eclipse.dataspaceconnector.core.controlplane.defaults.assetindex.InMemoryAssetIndex;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.Range;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.AssetEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
class ContractOfferServiceImplIntegrationTest {

    private final ContractDefinitionService contractDefinitionService = mock(ContractDefinitionService.class);
    private final ParticipantAgentService agentService = mock(ParticipantAgentService.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private AssetIndex assetIndex;
    private ContractOfferService contractOfferService;

    @BeforeEach
    void setUp() {
        assetIndex = new InMemoryAssetIndex();
        contractOfferService = new ContractOfferServiceImpl(agentService, contractDefinitionService, assetIndex, policyStore);
    }

    @Test
    void shouldLimitResult_withHeterogenousChunks() {
        var assets1 = range(10, 24).mapToObj(i -> createAsset("asset" + i).build()).collect(Collectors.toList());
        var assets2 = range(24, 113).mapToObj(i -> createAsset("asset" + i).build()).collect(Collectors.toList());
        var assets3 = range(113, 178).mapToObj(i -> createAsset("asset" + i).build()).collect(Collectors.toList());

        store(assets1);
        store(assets2);
        store(assets3);

        var def1 = getContractDefBuilder("def1").selectorExpression(selectorFrom(assets1)).build();
        var def2 = getContractDefBuilder("def2").selectorExpression(selectorFrom(assets2)).build();
        var def3 = getContractDefBuilder("def3").selectorExpression(selectorFrom(assets3)).build();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenAnswer(i -> Stream.of(def1, def2, def3));

        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());

        var query = ContractOfferQuery.builder().claimToken(ClaimToken.Builder.newInstance().build()).build();

        var from = 20;
        var to = 50;

        assertThat(contractOfferService.queryContractOffers(query, new Range(from, to))).hasSize(to - from);
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionService, times(1)).definitionsFor(isA(ParticipantAgent.class));
        verify(policyStore).findById("contract");
    }

    @Test
    void shouldLimitResult_insufficientAssets() {
        var assets1 = range(0, 12).mapToObj(i -> createAsset("asset" + i).build()).collect(Collectors.toList());
        var assets2 = range(12, 18).mapToObj(i -> createAsset("asset" + i).build()).collect(Collectors.toList());

        store(assets1);
        store(assets2);

        var def1 = getContractDefBuilder("def1").selectorExpression(selectorFrom(assets1)).build();
        var def2 = getContractDefBuilder("def2").selectorExpression(selectorFrom(assets2)).build();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenAnswer(i -> Stream.of(def1, def2));
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());


        var query = ContractOfferQuery.builder().claimToken(ClaimToken.Builder.newInstance().build()).build();

        var from = 14;
        var to = 50;

        // 4 definitions, 10 assets each = 40 offers total -> offset 20 ==> result = 20
        assertThat(contractOfferService.queryContractOffers(query, new Range(from, to))).hasSize(4);
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionService).definitionsFor(isA(ParticipantAgent.class));
        verify(policyStore, atLeastOnce()).findById("contract");
    }

    @Test
    void shouldLimitResult_pageOffsetLargerThanNumAssets() {
        var contractDefinition = range(0, 2).mapToObj(i -> getContractDefBuilder(String.valueOf(i))
                .build());

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenAnswer(i -> contractDefinition);

        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());

        var query = ContractOfferQuery.builder().claimToken(ClaimToken.Builder.newInstance().build()).build();

        var from = 25;
        var to = 50;

        // 2 definitions, 10 assets each = 20 offers total -> offset of 25 is outside
        assertThat(contractOfferService.queryContractOffers(query, new Range(from, to))).isEmpty();
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionService).definitionsFor(isA(ParticipantAgent.class));
        verify(policyStore, never()).findById("contract");
    }

    private void store(Collection<Asset> assets) {
        assets.stream().map(a -> new AssetEntry(a, DataAddress.Builder.newInstance().type("test-type").build()))
                .forEach(assetIndex::accept);
    }

    private AssetSelectorExpression selectorFrom(Collection<Asset> assets1) {
        var builder = AssetSelectorExpression.Builder.newInstance();
        var ids = assets1.stream().map(a -> a.getId()).collect(Collectors.toList());
        return builder.criteria(List.of(new Criterion(Asset.PROPERTY_ID, "in", ids))).build();
    }

    private ContractDefinition.Builder getContractDefBuilder(String id) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .selectorExpression(AssetSelectorExpression.SELECT_ALL);
    }

    private Asset.Builder createAsset(String id) {
        return Asset.Builder.newInstance().id(id).name("test asset " + id);
    }

}
