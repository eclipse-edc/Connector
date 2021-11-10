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
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFrameworkQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferQueryResponse;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferTemplate;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

class ContractOfferServiceImplTest {

    private ContractOfferFramework contractOfferFramework;
    private AssetIndex assetIndex;
    private ContractOfferService contractOfferService;

    @BeforeEach
    void setUp() {
        contractOfferFramework = mock(ContractOfferFramework.class);
        assetIndex = mock(AssetIndex.class);

        contractOfferService = new ContractOfferServiceImpl(contractOfferFramework, assetIndex);
    }

    @Test
    void testConstructorNullParametersThrowingIllegalArgumentException() {
        replay(contractOfferFramework, assetIndex);

        // just eval all constructor parameters are mandatory and lead to NPE
        assertThatThrownBy(() -> new ContractOfferServiceImpl(contractOfferFramework, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContractOfferServiceImpl(null, assetIndex))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContractOfferServiceImpl(null, null))
                .isInstanceOf(NullPointerException.class);

        verify(contractOfferFramework, assetIndex);
    }

    @Test
    void testContractOfferFrameworkReturningNullResultsEmptyStream() {
        //given
        ContractOfferQuery contractOfferQuery = ContractOfferQuery.builder().build();

        // expect
        expect(contractOfferFramework.queryTemplates(isA(ContractOfferFrameworkQuery.class)))
                .andReturn(null);

        replay(contractOfferFramework, assetIndex);

        // invocation
        ContractOfferQueryResponse response = contractOfferService.queryContractOffers(contractOfferQuery);

        // verification
        assertThat(response).isNotNull();
        assertThat(response.getContractOfferStream()).isEmpty();

        verify(contractOfferFramework, assetIndex);
    }

    @Test
    void testContractOfferFrameworkReturningEmptySelectorExpressionResultsEmptyStream() {
        //given
        ContractOfferQuery contractOfferQuery = ContractOfferQuery.builder().build();

        ContractOfferTemplate contractOfferTemplate = mock(ContractOfferTemplate.class);

        // expect
        expect(contractOfferFramework.queryTemplates(isA(ContractOfferFrameworkQuery.class)))
                .andReturn(Stream.of(contractOfferTemplate));
        expect(contractOfferTemplate.getSelectorExpression())
                .andReturn(null);

        replay(contractOfferFramework, assetIndex, contractOfferTemplate);

        // invocation
        ContractOfferQueryResponse response = contractOfferService.queryContractOffers(contractOfferQuery);

        // verification
        assertThat(response).isNotNull();
        assertThat(response.getContractOfferStream()).isEmpty();

        verify(contractOfferFramework, assetIndex, contractOfferTemplate);
    }


    @Test
    void testTargetAssetFilter() {

        // given
        ContractOfferTemplate t1 = mock(ContractOfferTemplate.class);
        AssetSelectorExpression s1 = AssetSelectorExpression.Builder.newInstance().build();
        ContractOfferTemplate t2 = mock(ContractOfferTemplate.class);
        AssetSelectorExpression s2 = AssetSelectorExpression.Builder.newInstance().build();

        Asset a1 = Asset.Builder.newInstance().id("a1").build();
        ContractOffer o1 = ContractOffer.Builder.newInstance().policy(Policy.Builder.newInstance().build()).assets(Collections.singletonList(a1)).build();
        ContractOffer o1Copy = ContractOffer.Builder.newInstance().policy(Policy.Builder.newInstance().build()).assets(Collections.singletonList(a1)).build();
        Asset a2 = Asset.Builder.newInstance().id("a2").build();
        ContractOffer o2 = ContractOffer.Builder.newInstance().policy(Policy.Builder.newInstance().build()).assets(Collections.singletonList(a2)).build();

        // expect
        EasyMock.expect(t1.getSelectorExpression()).andReturn(s1);
        EasyMock.expect(t2.getSelectorExpression()).andReturn(s2);

        EasyMock.expect(contractOfferFramework.queryTemplates(isA(ContractOfferFrameworkQuery.class)))
                .andReturn(Stream.of(t1, t2));

        Stream<Asset> stream1 = Stream.of(a1);
        Stream<Asset> stream2 = Stream.of(a2);
        EasyMock.expect(assetIndex.queryAssets(s1)).andReturn(stream1);
        EasyMock.expect(assetIndex.queryAssets(s2)).andReturn(stream2);

        EasyMock.expect(t1.getTemplatedOffers(stream1)).andReturn(Stream.of(o1, o1Copy));
        EasyMock.expect(t2.getTemplatedOffers(stream2)).andReturn(Stream.of(o2));

        replay(contractOfferFramework, assetIndex, t1, t2);

        // invocation
        ContractOfferQuery contractOfferQuery = ContractOfferQuery.builder().targetAsset(a1.getId()).build();
        ContractOfferQueryResponse response = contractOfferService.queryContractOffers(contractOfferQuery);

        // verification
        assertThat(response).isNotNull();
        Assertions.assertEquals(2, response.getContractOfferStream().count());

        verify(contractOfferFramework, assetIndex, t1, t2);
    }

    @Test
    void testFullFlow() {
        //given
        ContractOfferQuery contractOfferQuery = ContractOfferQuery.builder().build();

        ContractOfferTemplate contractOfferTemplate = mock(ContractOfferTemplate.class);
        AssetSelectorExpression assetSelectorExpression = AssetSelectorExpression.Builder.newInstance().build();
        Asset asset = mock(Asset.class);
        Stream<Asset> assetStream = Stream.of(asset);
        ContractOffer contractOffer = mock(ContractOffer.class);

        // expect
        expect(contractOfferFramework.queryTemplates(
                isA(ContractOfferFrameworkQuery.class))).andReturn(Stream.of(contractOfferTemplate));
        expect(contractOfferTemplate.getSelectorExpression())
                .andReturn(assetSelectorExpression);
        expect(assetIndex.queryAssets(assetSelectorExpression))
                .andReturn(assetStream);
        expect(contractOfferTemplate.getTemplatedOffers(assetStream))
                .andReturn(Stream.of(contractOffer));

        replay(contractOfferFramework, assetIndex, contractOfferTemplate, contractOffer);

        // invocation
        ContractOfferQueryResponse response = contractOfferService.queryContractOffers(contractOfferQuery);

        // verification
        assertThat(response).isNotNull();
        assertThat(response.getContractOfferStream()).containsExactly(contractOffer);

        verify(contractOfferFramework, assetIndex, contractOfferTemplate, contractOffer);
    }
}