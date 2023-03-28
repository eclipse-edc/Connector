/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.connector.contract.offer;

import org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionService;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferQuery;
import org.eclipse.edc.connector.contract.spi.offer.DatasetResolver;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.message.Range;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetResolverImplTest {

    private static final Range DEFAULT_RANGE = new Range(0, 10);
    
    private ParticipantAgentService agentService;
    private ContractDefinitionService contractDefinitionService;
    private AssetIndex assetIndex;
    private DataAddressResolver dataAddressResolver;
    private PolicyDefinitionStore policyStore;
    private Monitor monitor;

    private DatasetResolver datasetResolver;

    @BeforeEach
    void setUp() {
        agentService = mock(ParticipantAgentService.class);
        contractDefinitionService = mock(ContractDefinitionService.class);
        assetIndex = mock(AssetIndex.class);
        dataAddressResolver = mock(DataAddressResolver.class);
        policyStore = mock(PolicyDefinitionStore.class);
        monitor = mock(Monitor.class);
        datasetResolver = new DatasetResolverImpl(agentService, contractDefinitionService, assetIndex, dataAddressResolver, policyStore, monitor);
    
        when(agentService.createFor(isA(ClaimToken.class)))
                .thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(policyStore.findById(any()))
                .thenReturn(PolicyDefinition.Builder.newInstance()
                        .policy(Policy.Builder.newInstance().build())
                        .build());
        when(dataAddressResolver.resolveForAsset(any()))
                .thenReturn(DataAddress.Builder.newInstance()
                        .type("type")
                        .build());
    }

    @Test
    void queryDatasets_shouldGetDatasets() {
        var contractDefinition = getContractDefBuilder("1").build();
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenReturn(Stream.of(contractDefinition));
        
        var assetStream = Stream.of(Asset.Builder.newInstance().build(), Asset.Builder.newInstance().build());
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(assetStream);
        
        var query = ContractOfferQuery.builder()
                .claimToken(ClaimToken.Builder.newInstance().build())
                .provider(URI.create("urn:connector:edc-provider"))
                .consumer(URI.create("urn:connector:edc-consumer"))
                .build();

        var datasets = datasetResolver.queryDatasets(query);

        assertThat(datasets).hasSize(2);
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionService).definitionsFor(isA(ParticipantAgent.class));
        verify(assetIndex).queryAssets(isA(QuerySpec.class));
        verify(policyStore, atLeastOnce()).findById("contract");
    }

    @Test
    void queryDatasets_shouldNotGetDatasetIfPolicyIsNotFound() {
        var contractDefinition = getContractDefBuilder("1").build();
        
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenReturn(Stream.of(contractDefinition));
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(Stream.of(Asset.Builder.newInstance().build()));
        when(policyStore.findById(any())).thenReturn(null);
        
        var query = ContractOfferQuery.builder().claimToken(ClaimToken.Builder.newInstance().build()).build();

        var datasets = datasetResolver.queryDatasets(query);

        assertThat(datasets).isEmpty();
    }

    @Test
    void queryDatasets_shouldLimitResult() {
        var contractDefinition = range(0, 10)
                .mapToObj(i -> getContractDefBuilder(String.valueOf(i))
                .build());
        
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenAnswer(i -> contractDefinition);
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenAnswer(inv -> range(20, 50).mapToObj(i -> createAsset("asset" + i).build()));

        var from = 20;
        var to = 50;

        var datasets = datasetResolver.queryDatasets(getQuery(from, to));

        assertThat(datasets).hasSize(to - from)
                .extracting(dataset -> (String) dataset.getProperty(Asset.PROPERTY_ID))
                .allSatisfy(id -> {
                    var idNumber = Integer.valueOf(id.replace("asset", ""));
                    assertThat(idNumber).isStrictlyBetween(from - 1, to);
                });
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionService).definitionsFor(isA(ParticipantAgent.class));
        verify(assetIndex).queryAssets(isA(QuerySpec.class));
        verify(policyStore, atLeastOnce()).findById("contract");
    }

    @Test
    void queryDatasets_shouldLimitResult_insufficientAssets() {
        var contractDefinition = getContractDefBuilder("1").build();
        
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenReturn(Stream.of(contractDefinition));
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenAnswer(inv -> range(10, 20).mapToObj(i -> createAsset("asset" + i).build()));

        var from = 10;
        var to = 30;

        var datasets = datasetResolver.queryDatasets(getQuery(from, to));

        // 20 assets = 20 offers total -> offset 10 ==> result = 10
        assertThat(datasets).hasSize(10);
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionService).definitionsFor(isA(ParticipantAgent.class));
        verify(assetIndex, atLeastOnce()).queryAssets(isA(QuerySpec.class));
        verify(policyStore, atLeastOnce()).findById("contract");
    }

    @Test
    void queryDatasets_shouldGetDatasetsWithAssetFilteringApplied() {
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_NAME, "1").build())
                .validity(10)
                .build();
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class))).thenReturn(Stream.of(contractDefinition));
        
        var assetStream = Stream.of(Asset.Builder.newInstance().build(), Asset.Builder.newInstance().build());
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(assetStream);

        var query = ContractOfferQuery.builder()
                .range(DEFAULT_RANGE)
                .claimToken(ClaimToken.Builder.newInstance().build())
                .assetsCriteria(List.of(new Criterion(Asset.PROPERTY_ID, "=", "2")))
                .build();

        var datasets = datasetResolver.queryDatasets(query);

        assertThat(datasets).isEmpty();
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionService).definitionsFor(isA(ParticipantAgent.class));
        verify(policyStore, never()).findById("contract");
        var expectedQuerySpec = QuerySpec.Builder.newInstance()
                .filter(query.getAssetsCriteria())
                .range(DEFAULT_RANGE)
                .build();
        verify(assetIndex).queryAssets(expectedQuerySpec);
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
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .validity(TimeUnit.MINUTES.toSeconds(10));
    }

    private Asset.Builder createAsset(String id) {
        return Asset.Builder.newInstance()
                .id(id)
                .name("test asset " + id);
    }
}
