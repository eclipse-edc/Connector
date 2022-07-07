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

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.Range;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractOfferServiceImplTest {

    private static final Range DEFAULT_RANGE = new Range(0, 10);
    private final ContractDefinitionService contractDefinitionService = mock(ContractDefinitionService.class);
    private final AssetIndex assetIndex = mock(AssetIndex.class);
    private final ParticipantAgentService agentService = mock(ParticipantAgentService.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);

    private ContractOfferService contractOfferService;

    @BeforeEach
    void setUp() {
        contractOfferService = new ContractOfferServiceImpl(agentService, contractDefinitionService, assetIndex, policyStore);
    }

    @Test
    void shouldGetContractOffers() {
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class), any())).thenReturn(Stream.of(contractDefinition));
        var assetStream = Stream.of(Asset.Builder.newInstance().build(), Asset.Builder.newInstance().build());
        when(assetIndex.queryAssets(isA(AssetSelectorExpression.class))).thenReturn(assetStream);
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build());

        var query = ContractOfferQuery.builder().claimToken(ClaimToken.Builder.newInstance().build()).build();

        assertThat(contractOfferService.queryContractOffers(query, DEFAULT_RANGE)).hasSize(2);
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(contractDefinitionService).definitionsFor(isA(ParticipantAgent.class), eq(DEFAULT_RANGE));
        verify(assetIndex).queryAssets(isA(AssetSelectorExpression.class));
        verify(policyStore).findById("contract");
    }

    @Test
    void shouldNotGetContractOfferIfPolicyIsNotFound() {
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();
        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(contractDefinitionService.definitionsFor(isA(ParticipantAgent.class), any())).thenReturn(Stream.of(contractDefinition));
        when(assetIndex.queryAssets(isA(AssetSelectorExpression.class))).thenReturn(Stream.of(Asset.Builder.newInstance().build()));
        when(policyStore.findById(any())).thenReturn(null);

        var query = ContractOfferQuery.builder().claimToken(ClaimToken.Builder.newInstance().build()).build();

        var result = contractOfferService.queryContractOffers(query, DEFAULT_RANGE);

        assertThat(result).hasSize(0);
    }
}
