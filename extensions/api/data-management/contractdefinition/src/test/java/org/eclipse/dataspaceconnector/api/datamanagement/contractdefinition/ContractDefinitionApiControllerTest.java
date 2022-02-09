package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ContractDefinitionApiControllerTest {
    private ContractDefinitionApiController controller;

    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new ContractDefinitionApiController(monitor);
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
    void getContractDef_found() {
        //todo: implement
    }

    @Test
    void getContractDef_notFound() {
        assertThat(controller.getContractDefinition("not-exist")).isNull();
    }

    @Test
    void createContractDefinition_success() {
        //todo: implement

    }

    @Test
    void createContractDefinition_alreadyExists() {
        //todo: implement
    }

    @Test
    void createContractDefinition_policyNotFound() {
        //todo: implement

    }

    @Test
    void createContractDefinition_noAssetSelector() {
        //todo: implement
    }

    @Test
    void createContractDefinition_noAccessPolicy() {
        //todo: implement
    }

    @Test
    void createContractDefinition_noContractPolicy() {
        //todo: implement
    }

    @Test
    void delete() {
        //todo: implement
    }

    @Test
    void delete_notFound() {

    }

    @Test
    void delete_notPossible() {
        //todo: implement
    }
}
