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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractOfferServiceImplTest {

    private ContractOfferFramework contractOfferFramework;
    private AssetIndex assetIndex;
    private ContractOfferService contractOfferService;

    @BeforeEach
    void setUp() {
        contractOfferFramework = EasyMock.mock(ContractOfferFramework.class);
        assetIndex = EasyMock.mock(AssetIndex.class);

        contractOfferService = new ContractOfferServiceImpl(contractOfferFramework, assetIndex);
    }

    @Test
    void testConstructorNullParametersThrowingIllegalArgumentException() {
        EasyMock.replay(contractOfferFramework, assetIndex);

        // just eval all constructor parameters are mandatory and lead to NPE
        assertThatThrownBy(() -> new ContractOfferServiceImpl(contractOfferFramework, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContractOfferServiceImpl(null, assetIndex))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContractOfferServiceImpl(null, null))
                .isInstanceOf(NullPointerException.class);

        EasyMock.verify(contractOfferFramework, assetIndex);
    }

    @Test
    void testContractOfferFrameworkReturningNullResultsEmptyStream() {
        //given
        final ContractOfferQuery contractOfferQuery = ContractOfferQuery.builder().build();

        // expect
        EasyMock.expect(contractOfferFramework.queryTemplates(EasyMock.isA(ContractOfferFrameworkQuery.class)))
                .andReturn(null);

        EasyMock.replay(contractOfferFramework, assetIndex);

        // invocation
        final ContractOfferQueryResponse response = contractOfferService.queryContractOffers(contractOfferQuery);

        // verification
        assertThat(response).isNotNull();
        assertThat(response.getContractOfferStream()).isEmpty();

        EasyMock.verify(contractOfferFramework, assetIndex);
    }

    @Test
    void testContractOfferFrameworkReturningEmptySelectorExpressionResultsEmptyStream() {
        //given
        final ContractOfferQuery contractOfferQuery = ContractOfferQuery.builder().build();

        final ContractOfferTemplate contractOfferTemplate = EasyMock.mock(ContractOfferTemplate.class);

        // expect
        EasyMock.expect(contractOfferFramework.queryTemplates(EasyMock.isA(ContractOfferFrameworkQuery.class)))
                .andReturn(Stream.of(contractOfferTemplate));
        EasyMock.expect(contractOfferTemplate.getSelectorExpression())
                .andReturn(Optional.empty());
        EasyMock.expect(contractOfferTemplate.getTemplatedOffers(EasyMock.isA(Stream.class)))
                .andReturn(Stream.empty());

        EasyMock.replay(contractOfferFramework, assetIndex, contractOfferTemplate);

        // invocation
        final ContractOfferQueryResponse response = contractOfferService.queryContractOffers(contractOfferQuery);

        // verification
        assertThat(response).isNotNull();
        assertThat(response.getContractOfferStream()).isEmpty();

        EasyMock.verify(contractOfferFramework, assetIndex, contractOfferTemplate);
    }

    @Test
    void testFullFlow() {
        //given
        final ContractOfferQuery contractOfferQuery = ContractOfferQuery.builder().build();

        final ContractOfferTemplate contractOfferTemplate = EasyMock.mock(ContractOfferTemplate.class);
        final AssetSelectorExpression assetSelectorExpression = AssetSelectorExpression.builder().build();
        final Asset asset = EasyMock.mock(Asset.class);
        final Stream<Asset> assetStream = Stream.of(asset);
        final ContractOffer contractOffer = EasyMock.mock(ContractOffer.class);

        // expect
        EasyMock.expect(contractOfferFramework.queryTemplates(
                EasyMock.isA(ContractOfferFrameworkQuery.class))).andReturn(Stream.of(contractOfferTemplate));
        EasyMock.expect(contractOfferTemplate.getSelectorExpression())
                .andReturn(Optional.of(assetSelectorExpression));
        EasyMock.expect(assetIndex.queryAssets(assetSelectorExpression))
                .andReturn(assetStream);
        EasyMock.expect(contractOfferTemplate.getTemplatedOffers(assetStream))
                .andReturn(Stream.of(contractOffer));

        EasyMock.replay(contractOfferFramework, assetIndex, contractOfferTemplate, contractOffer);

        // invocation
        final ContractOfferQueryResponse response = contractOfferService.queryContractOffers(contractOfferQuery);

        // verification
        assertThat(response).isNotNull();
        assertThat(response.getContractOfferStream()).containsExactly(contractOffer);

        EasyMock.verify(contractOfferFramework, assetIndex, contractOfferTemplate, contractOffer);
    }
}