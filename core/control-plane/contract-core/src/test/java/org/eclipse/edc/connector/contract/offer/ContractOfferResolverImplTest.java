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

import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionService;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferQuery;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferResolver;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.message.Range;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractOfferResolverImplTest {

    private static final Range DEFAULT_RANGE = new Range(0, 10);
    private final ContractDefinitionService contractDefinitionService = mock(ContractDefinitionService.class);
    private final AssetIndex assetIndex = mock(AssetIndex.class);
    private final ParticipantAgentService agentService = mock(ParticipantAgentService.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);

    private ContractOfferResolver contractOfferResolver;

    @BeforeEach
    void setUp() {
        contractOfferResolver = new ContractOfferResolverImpl(agentService, contractDefinitionService, assetIndex, policyStore);
    }

    @Test
    void shouldGetContractOffers() {
        var contractDefinition = getContractDefBuilder("1")
                .build();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenReturn(Stream.of(contractDefinition));
        var assetStream = Stream.of(Asset.Builder.newInstance().build(), Asset.Builder.newInstance().build());
        when(assetIndex.countAssets(any())).thenReturn(2L);
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(assetStream);
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());

        var offers = contractOfferResolver.queryContractOffers(getQuery());

        assertThat(offers).hasSize(2);
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionService).definitionsFor(isA(ParticipantAgent.class));
        verify(assetIndex).queryAssets(isA(QuerySpec.class));
        verify(policyStore, atLeastOnce()).findById("contract");
    }

    @Test
    void shouldNotGetContractOfferIfPolicyIsNotFound() {
        var contractDefinition = getContractDefBuilder("1")
                .build();
        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenReturn(Stream.of(contractDefinition));
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(Stream.of(Asset.Builder.newInstance().build()));
        when(policyStore.findById(any())).thenReturn(null);

        var result = contractOfferResolver.queryContractOffers(getQuery());

        assertThat(result).hasSize(0);
    }

    @Test
    void shouldLimitResult() {
        var contractDefinition = range(0, 10).mapToObj(i -> getContractDefBuilder(String.valueOf(i))
                .build());

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenAnswer(i -> contractDefinition);
        when(assetIndex.countAssets(any())).thenReturn(100L);
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenAnswer(inv -> range(20, 50).mapToObj(i -> createAsset("asset" + i).build()));
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());

        var from = 20;
        var to = 50;

        var offers = contractOfferResolver.queryContractOffers(getQuery(from, to));

        assertThat(offers).hasSize(to - from)
                .extracting(ContractOffer::getAsset)
                .extracting(Asset::getId)
                .allSatisfy(id -> {
                    var idNumber = Integer.valueOf(id.replace("asset", ""));
                    assertThat(idNumber).isStrictlyBetween(from - 1, to);
                });
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionService).definitionsFor(isA(ParticipantAgent.class));
        verify(assetIndex).queryAssets(isA(QuerySpec.class));
        verify(policyStore).findById("contract");
    }


    @Test
    void shouldNotLimitResult_whenAssetsAreLessThanTheRequested() {
        var contractDefinition = getContractDefBuilder("1").build();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenReturn(Stream.of(contractDefinition));
        when(assetIndex.countAssets(any())).thenReturn(40L);
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenAnswer(inv -> range(20, 50).mapToObj(i -> createAsset("asset" + i).build()));
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());

        var from = 20;
        var to = 80;

        var result = contractOfferResolver.queryContractOffers(getQuery(from, to));

        assertThat(result).isNotEmpty();
        verify(assetIndex, times(1)).queryAssets(and(isA(QuerySpec.class), argThat(it -> it.getLimit() == 40)));
    }

    @Test
    void shouldLimitResult_insufficientAssets() {
        var contractDefinition = range(0, 4).mapToObj(i -> getContractDefBuilder(String.valueOf(i))
                .build());

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenAnswer(i -> contractDefinition);
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenAnswer(inv -> range(0, 10).mapToObj(i -> createAsset("asset" + i).build()));
        when(assetIndex.countAssets(any())).thenReturn(10L);
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());

        var from = 20;
        var to = 50;

        var offers = contractOfferResolver.queryContractOffers(getQuery(from, to));

        // 4 definitions, 10 assets each = 40 offers total -> offset 20 ==> result = 20
        assertThat(offers).hasSize(20);
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionService).definitionsFor(isA(ParticipantAgent.class));
        verify(assetIndex, atLeastOnce()).queryAssets(isA(QuerySpec.class));
        verify(policyStore, atLeastOnce()).findById("contract");
    }

    @Test
    void shouldLimitResult_pageOffsetLargerThanNumAssets() {
        var contractDefinition = range(0, 2).mapToObj(i -> getContractDefBuilder(String.valueOf(i))
                .build());

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenAnswer(i -> contractDefinition);
        when(assetIndex.countAssets(any())).thenReturn(10L);
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenAnswer(inv -> range(0, 10).mapToObj(i -> createAsset("asset" + i).build()));
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());

        var from = 25;
        var to = 50;

        var offers = contractOfferResolver.queryContractOffers(getQuery(from, to));

        // 2 definitions, 10 assets each = 20 offers total -> offset of 25 is outside
        assertThat(offers).isEmpty();
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionService).definitionsFor(isA(ParticipantAgent.class));
        verify(assetIndex, never()).queryAssets(isA(QuerySpec.class));
        verify(policyStore, never()).findById("contract");
    }

    @Test
    void shouldLimitResultOfSingleAssetForContractDefinition() {
        var contractDefinitions = range(0, 80)
                .mapToObj(i -> getContractDefBuilder(String.valueOf(i)).build());

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenAnswer(i -> contractDefinitions);
        when(assetIndex.countAssets(any())).thenReturn(1L);
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenAnswer(inv -> Stream.of(createAsset("asset").build()));
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());

        var from = 20;
        var to = 50;

        var offers = contractOfferResolver.queryContractOffers(getQuery(from, to));

        assertThat(offers).hasSize(to - from);
        verify(contractDefinitionService).definitionsFor(isA(ParticipantAgent.class));
        verify(assetIndex, times(30)).queryAssets(isA(QuerySpec.class));
        verify(policyStore, times(30)).findById("contract");
    }

    @Test
    void shouldGetContractOffersWithAssetFilteringApplied() {
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_NAME, "1").build())
                .build();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenReturn(Stream.of(contractDefinition));
        var assetStream = Stream.of(Asset.Builder.newInstance().build(), Asset.Builder.newInstance().build());
        when(assetIndex.countAssets(isA(QuerySpec.class))).thenReturn(1000L);
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(assetStream);
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());

        var query = ContractOfferQuery.builder()
                .range(DEFAULT_RANGE)
                .claimToken(ClaimToken.Builder.newInstance().build())
                .assetsCriteria(List.of(new Criterion(Asset.PROPERTY_ID, "=", "2")))
                .build();

        var offers = contractOfferResolver.queryContractOffers(query);

        assertThat(offers).hasSize(2);
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionService).definitionsFor(isA(ParticipantAgent.class));
        verify(policyStore).findById("contract");
        var expectedQuerySpec = QuerySpec.Builder.newInstance()
                .filter(concat(contractDefinition.getSelectorExpression().getCriteria().stream(), query.getAssetsCriteria().stream()).collect(Collectors.toList()))
                .range(DEFAULT_RANGE)
                .build();
        verify(assetIndex).queryAssets(eq(expectedQuerySpec));
    }

    @NotNull
    private ContractOfferQuery getQuery() {
        return ContractOfferQuery.builder().claimToken(ClaimToken.Builder.newInstance().build()).build();
    }

    private ContractOfferQuery getQuery(int from, int to) {
        return ContractOfferQuery.builder()
                .range(new Range(from, to))
                .claimToken(ClaimToken.Builder.newInstance().build())
                .build();
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
