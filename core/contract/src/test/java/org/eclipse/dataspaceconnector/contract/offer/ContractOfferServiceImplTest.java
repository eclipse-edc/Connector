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

package org.eclipse.dataspaceconnector.contract.offer;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

class ContractOfferServiceImplTest {

    private ContractDefinitionService contractDefinitionService;
    private AssetIndex assetIndex;
    private ContractOfferService contractOfferService;
    private ParticipantAgentService agentService;

    @BeforeEach
    void setUp() {
        contractDefinitionService = mock(ContractDefinitionService.class);
        assetIndex = mock(AssetIndex.class);
        agentService = mock(ParticipantAgentService.class);

        contractOfferService = new ContractOfferServiceImpl(agentService, contractDefinitionService, assetIndex);
    }

    @Test
    void testConstructorNullParametersThrowingIllegalArgumentException() {
        replay(contractDefinitionService, assetIndex);

        // just eval all constructor parameters are mandatory and lead to NPE
        assertThatThrownBy(() -> new ContractOfferServiceImpl(null, contractDefinitionService, assetIndex))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContractOfferServiceImpl(agentService, contractDefinitionService, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContractOfferServiceImpl(agentService, null, assetIndex))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContractOfferServiceImpl(agentService, null, null))
                .isInstanceOf(NullPointerException.class);

        verify(contractDefinitionService, assetIndex);
    }


    @Test
    void testFullFlow() {
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(Policy.Builder.newInstance().build())
                .contractPolicy(Policy.Builder.newInstance().build())
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();

        EasyMock.expect(agentService.createFor(EasyMock.isA(ClaimToken.class))).andReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        EasyMock.expect(contractDefinitionService.definitionsFor(EasyMock.isA(ParticipantAgent.class))).andReturn(Stream.of(contractDefinition));
        var assetStream = Stream.of(Asset.Builder.newInstance().build(), Asset.Builder.newInstance().build());
        EasyMock.expect(assetIndex.queryAssets(EasyMock.isA(AssetSelectorExpression.class))).andReturn(assetStream);

        EasyMock.replay(agentService, contractDefinitionService, assetIndex);

        ContractOfferQuery query = ContractOfferQuery.builder().claimToken(ClaimToken.Builder.newInstance().build()).build();

        // collect() instead of count() forces iteration
        //noinspection SimplifyStreamApiCallChains
        assertThat((int) contractOfferService.queryContractOffers(query).collect(Collectors.toList()).size()).isEqualTo(2);

        EasyMock.verify(contractDefinitionService, assetIndex);
    }
}
