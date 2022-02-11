package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TransferProcessApiApiControllerTest {
    private TransferProcessApiController controller;

    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new TransferProcessApiController(monitor);
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
    void getTransferProcess_found() {
        //todo: implement
    }

    @Test
    void getTransferProcess_notFound() {
        assertThat(controller.getTransferProcess("not-exist")).isNull();
    }

    @Test
    void cancelTransferProcess_success() {
        //todo: implement

    }


    @Test
    void cancelContractDefinition_notFound() {
        //todo: implement
    }

    @Test
    void cancelContractDefinition_alreadyCancelled() {
        //todo: implement
    }

    @Test
    void cancelContractDefinition_notPossible() {
        // not possible when in state CANCELLED, ERROR, DEPROVISIONING, DEPROVISIONING_REQ, DEPROVISIONED, COMPLETE or ENDED
        //todo: implement
    }

}
