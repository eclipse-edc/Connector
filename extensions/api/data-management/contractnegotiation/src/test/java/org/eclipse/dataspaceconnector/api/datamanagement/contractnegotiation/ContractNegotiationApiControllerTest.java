/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 *    Microsoft Corporation - Added initiate-negotiation endpoint tests
 */


package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation;

import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.ContractAgreementDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ContractNegotiationApiControllerTest {
    private ContractNegotiationApiController controller;

    public static Stream<Arguments> getInvalidNegotiationParameters() {
        return Stream.of(Arguments.of(null, "consumer", "ids-multipart", "test-offer"),
                Arguments.of("", "consumer", "ids-multipart", "test-offer"),
                Arguments.of("  ", "consumer", "ids-multipart", "test-offer"),
                Arguments.of("http://some-connector", null, "ids-multipart", "test-offer"),
                Arguments.of("http://some-connector", "", "ids-multipart", "test-offer"),
                Arguments.of("http://some-connector", "  ", "ids-multipart", "test-offer"),
                Arguments.of("http://some-connector", "consumer", null, "test-offer"),
                Arguments.of("http://some-connector", "consumer", "", "test-offer"),
                Arguments.of("http://some-connector", "consumer", "   ", "test-offer"),
                Arguments.of("http://some-connector", "consumer", "ids-multipart", null),
                Arguments.of("http://some-connector", "consumer", "ids-multipart", ""),
                Arguments.of("http://some-connector", "consumer", "ids-multipart", "  ")
        );
    }

    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new ContractNegotiationApiController(monitor);
    }

    @Test
    void getAll_paging_noFilter() {
        //todo: implement
    }

    @Test
    void getAll_paging_pageSizeTooLarge() {
        //todo: implement
    }

    @Test
    void getAll_paging_offsetOutOfBounds() {
        //todo: implement
    }

    @ParameterizedTest
    @ValueSource(strings = { "id=id1", "id = id1", "id =id1", "id= id1" })
    void getAll_paging_withValidFilter(String filter) {
        //todo: implement
    }

    @ParameterizedTest
    @ValueSource(strings = { "id > id1", "id < id1", "id like id1", "id inside id1" })
    void getAll_paging_filterWithInvalidOperator(String filter) {
        //todo: implement
    }

    @ParameterizedTest
    @ValueSource(strings = { "id>id1", "id<id1" })
    void getAll_paging_withInvalidFilter(String filter) {
        //todo: implement
    }

    @Test
    void getAll_paging_invalidParams() {
        //todo: implement
    }

    @Test
    void getAll_noPaging() {
        //todo: implement
    }

    @Test
    void getContractNegotiation_found() {
        //todo: implement
    }

    @Test
    void getContractNegotiation_notFound() {
        assertThat(controller.getNegotiation("not-exist")).isNull();
    }

    @Test
    void getContractNegotiationState_found() {
        //todo: implement
    }

    @Test
    void getContractNegotiationState_notFound() {
        assertThat(controller.getNegotiation("not-exist")).isNull();
    }

    @Test
    void cancel() {
        //todo: implement
    }

    @Test
    void cancel_notFound() {
        //todo: implement
    }

    @Test
    void cancel_notPossible() {
        //todo: implement
    }

    @Test
    void decline() {
        //todo: implement
    }

    @Test
    void decline_notFound() {
        //todo: implement
    }

    @Test
    void decline_notPossible() {
        //todo: implement
    }

    @Test
    void getAgreementForNegotiation() {
        var agreement = controller.getAgreementForNegotiation("test-negotiation");
        assertThat(agreement)
                .isNotNull()
                .extracting(ContractAgreementDto::getNegotiationId)
                .isEqualTo("test-negotiation");
    }

    @Test
    void getAgreementForNegotiation_negotiationNotExist() {
        //todo: implement
    }

    @Test
    void initiateNegotiation() {
        //todo implement
    }


    @ParameterizedTest
    @MethodSource("getInvalidNegotiationParameters")
    void initiateNegotiation_invalidRequestBody(String connectorAddress, String connectorId, String protocol, String offerId) {
        var rq = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorAddress(connectorAddress)
                .connectorId(connectorId)
                .protocol(protocol)
                .offerId(offerId)
                .build();
        assertThatThrownBy(() -> controller.initiateContractNegotiation(rq)).isInstanceOf(IllegalArgumentException.class);
    }
}
