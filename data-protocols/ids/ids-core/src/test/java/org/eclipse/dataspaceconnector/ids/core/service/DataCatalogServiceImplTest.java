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
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferQueryResponse;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataCatalogServiceImplTest {
    private static final String CATALOG_ID = "catalogId";

    // subject
    private DataCatalogServiceImpl dataCatalogService;

    // mocks
    private Monitor monitor;
    private ContractOfferService contractOfferService;

    @BeforeEach
    void setUp() {
        monitor = EasyMock.createMock(Monitor.class);
        contractOfferService = EasyMock.createMock(ContractOfferService.class);
        dataCatalogService = new DataCatalogServiceImpl(monitor, CATALOG_ID, contractOfferService);
    }

    @Test
    void getDataCatalog() {
        // prepare
        VerificationResult verificationResult = EasyMock.createMock(VerificationResult.class);

        List<org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer> offers = Arrays.asList(
                org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer.Builder.newInstance()
                        .policy(Policy.Builder.newInstance().build())
                        .build(),
                org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer.Builder.newInstance()
                        .policy(Policy.Builder.newInstance().build())
                        .build());
        ContractOfferQueryResponse response = EasyMock.createMock(ContractOfferQueryResponse.class);
        EasyMock.expect(response.getContractOfferStream()).andReturn(offers.stream());
        EasyMock.expect(contractOfferService.queryContractOffers(EasyMock.anyObject(ContractOfferQuery.class)))
                .andReturn(response);

        // record
        EasyMock.replay(monitor, response, contractOfferService);

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