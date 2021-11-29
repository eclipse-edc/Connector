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

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogServiceImplTest {
    private static final String CATALOG_ID = "catalogId";

    // subject
    private CatalogServiceImpl dataCatalogService;

    // mocks
    private Monitor monitor;
    private ContractOfferService contractOfferService;

    @BeforeEach
    void setUp() {
        monitor = EasyMock.createMock(Monitor.class);
        contractOfferService = EasyMock.createMock(ContractOfferService.class);
        dataCatalogService = new CatalogServiceImpl(monitor, CATALOG_ID, contractOfferService);
    }

    @Test
    void getDataCatalog() {
        // prepare
        VerificationResult verificationResult = EasyMock.createMock(VerificationResult.class);

        var offers = Arrays.asList(
                ContractOffer.Builder.newInstance()
                        .policy(Policy.Builder.newInstance().build())
                        .id("1")
                        .build(),
                ContractOffer.Builder.newInstance()
                        .policy(Policy.Builder.newInstance().build())
                        .id("1")
                        .build());
        EasyMock.expect(contractOfferService.queryContractOffers(EasyMock.anyObject(ContractOfferQuery.class))).andReturn(offers.stream());

        // record
        EasyMock.replay(monitor, contractOfferService);

        // invoke
        var result = dataCatalogService.getDataCatalog(verificationResult);

        // verify
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CATALOG_ID);
        assertThat(result.getContractOffers()).hasSameElementsAs(offers);
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(monitor);
    }
}
