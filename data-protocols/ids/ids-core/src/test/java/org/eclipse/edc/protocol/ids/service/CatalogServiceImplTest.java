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

package org.eclipse.edc.protocol.ids.service;

import org.eclipse.edc.connector.contract.spi.offer.ContractOfferQuery;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferResolver;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.ids.spi.types.container.DescriptionRequest;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogServiceImplTest {
    private static final String CATALOG_ID = "catalogId";

    private final ContractOfferResolver contractOfferResolver = mock(ContractOfferResolver.class);
    private CatalogServiceImpl dataCatalogService;

    @BeforeEach
    void setUp() {
        dataCatalogService = new CatalogServiceImpl(CATALOG_ID, contractOfferResolver);
    }

    @Test
    void getDataCatalog() {
        var claimToken = ClaimToken.Builder.newInstance().build();

        var offers = Arrays.asList(createContractOffer("1"), createContractOffer("2"));
        when(contractOfferResolver.queryContractOffers(any(ContractOfferQuery.class))).thenReturn(offers.stream());
        var descriptionRequest = DescriptionRequest.Builder.newInstance()
                .claimToken(claimToken)
                .querySpec(QuerySpec.none())
                .build();

        var result = dataCatalogService.getDataCatalog(descriptionRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CATALOG_ID);
        assertThat(result.getContractOffers()).hasSameElementsAs(offers);
        verify(contractOfferResolver).queryContractOffers(any(ContractOfferQuery.class));
    }

    private static ContractOffer createContractOffer(String id) {
        return ContractOffer.Builder.newInstance()
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().id("test-asset").build())
                .id(id)
                .contractStart(ZonedDateTime.now().toInstant().toEpochMilli())
                .contractEnd(ZonedDateTime.now().plusMonths(1).toInstant().toEpochMilli())
                .build();
    }
}
