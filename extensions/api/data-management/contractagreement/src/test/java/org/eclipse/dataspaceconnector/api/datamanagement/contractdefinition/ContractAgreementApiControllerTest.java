/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ContractAgreementApiControllerTest {
    private ContractAgreementApiController controller;

    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new ContractAgreementApiController(monitor);
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
    void getContractAgreement_found() {
        //todo: implement
    }

    @Test
    void getContractAgreement_notFound() {
        assertThat(controller.getContractAgreement("not-exist")).isNull();
    }

}