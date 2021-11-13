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

package org.eclipse.dataspaceconnector.contract;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.contract.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.contract.ParticipantAgentService;
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

    private ContractOfferFramework contractOfferFramework;
    private AssetIndex assetIndex;
    private ContractOfferService contractOfferService;
    private ParticipantAgentService agentService;

    @BeforeEach
    void setUp() {
        contractOfferFramework = mock(ContractOfferFramework.class);
        assetIndex = mock(AssetIndex.class);
        agentService = mock(ParticipantAgentService.class);

        contractOfferService = new ContractOfferServiceImpl(agentService, contractOfferFramework, assetIndex);
    }

    @Test
    void testConstructorNullParametersThrowingIllegalArgumentException() {
        replay(contractOfferFramework, assetIndex);

        // just eval all constructor parameters are mandatory and lead to NPE
        assertThatThrownBy(() -> new ContractOfferServiceImpl(null, contractOfferFramework, assetIndex))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContractOfferServiceImpl(agentService, contractOfferFramework, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContractOfferServiceImpl(agentService, null, assetIndex))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContractOfferServiceImpl(agentService, null, null))
                .isInstanceOf(NullPointerException.class);

        verify(contractOfferFramework, assetIndex);
    }


    @Test
    void testFullFlow() {
        var contractDefinition = new ContractDefinition(Policy.Builder.newInstance().build(), AssetSelectorExpression.SELECT_ALL);
        EasyMock.expect(agentService.createFor(EasyMock.isA(ClaimToken.class))).andReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        EasyMock.expect(contractOfferFramework.definitionsFor(EasyMock.isA(ParticipantAgent.class))).andReturn(Stream.of(contractDefinition));
        EasyMock.expect(assetIndex.queryAssets(EasyMock.isA(AssetSelectorExpression.class))).andReturn(Stream.of(Asset.Builder.newInstance().build()));

        EasyMock.replay(agentService, contractOfferFramework, assetIndex);

        ContractOfferQuery query = ContractOfferQuery.builder().claimToken(ClaimToken.Builder.newInstance().build()).build();

        // collect() instead of count() forces iteration
        //noinspection SimplifyStreamApiCallChains
        assertThat((int) contractOfferService.queryContractOffers(query).collect(Collectors.toList()).size()).isEqualTo(1);

        EasyMock.verify(contractOfferFramework, assetIndex);
    }
}
