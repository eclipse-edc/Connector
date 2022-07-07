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

package org.eclipse.dataspaceconnector.ids.core.service;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.Range;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogServiceImplTest {
    private static final String CATALOG_ID = "catalogId";

    private final ContractOfferService contractOfferService = mock(ContractOfferService.class);
    private CatalogServiceImpl dataCatalogService;

    @BeforeEach
    void setUp() {
        dataCatalogService = new CatalogServiceImpl(mock(Monitor.class), CATALOG_ID, contractOfferService);
    }

    @Test
    void getDataCatalog() {
        var claimToken = ClaimToken.Builder.newInstance().build();

        var offers = Arrays.asList(
                ContractOffer.Builder.newInstance()
                        .policy(Policy.Builder.newInstance().build())
                        .id("1")
                        .build(),
                ContractOffer.Builder.newInstance()
                        .policy(Policy.Builder.newInstance().build())
                        .id("1")
                        .build());
        when(contractOfferService.queryContractOffers(any(ContractOfferQuery.class), any())).thenReturn(offers.stream());

        var result = dataCatalogService.getDataCatalog(claimToken, new Range(0, 100));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CATALOG_ID);
        assertThat(result.getContractOffers()).hasSameElementsAs(offers);
        verify(contractOfferService).queryContractOffers(any(ContractOfferQuery.class), any());
    }

}
