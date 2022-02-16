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
 *   ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ContractNegotiationApiControllerTest {
    private ContractNegotiationController controller;

    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new ContractNegotiationController(monitor);
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
}
