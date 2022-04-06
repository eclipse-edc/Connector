/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.flow;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResult;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataFlowManagerImplTest {

    @Test
    void should_initiate_flow_on_correct_controller() {
        var manager = new DataFlowManagerImpl();
        var controller = mock(DataFlowController.class);
        var dataRequest = DataRequest.Builder.newInstance().destinationType("type").build();
        var policy = Policy.Builder.newInstance().build();

        when(controller.canHandle(any())).thenReturn(true);
        when(controller.initiateFlow(any(), any())).thenReturn(DataFlowInitiateResult.success("success"));
        manager.register(controller);

        var response = manager.initiate(dataRequest, policy);

        assertThat(response.succeeded()).isTrue();
    }

    @Test
    void should_return_fatal_error_if_no_controller_can_handle_the_request() {
        var manager = new DataFlowManagerImpl();
        var controller = mock(DataFlowController.class);
        var dataRequest = DataRequest.Builder.newInstance().destinationType("type").build();
        var policy = Policy.Builder.newInstance().build();

        when(controller.canHandle(any())).thenReturn(false);
        manager.register(controller);

        var response = manager.initiate(dataRequest, policy);

        assertThat(response.succeeded()).isFalse();
        assertThat(response.getFailure().status()).isEqualTo(FATAL_ERROR);
    }

    @Test
    void should_catch_exceptions_and_return_fatal_error() {
        var manager = new DataFlowManagerImpl();
        var controller = mock(DataFlowController.class);
        var dataRequest = DataRequest.Builder.newInstance().destinationType("type").build();
        var policy = Policy.Builder.newInstance().build();

        when(controller.canHandle(any())).thenReturn(true);
        when(controller.initiateFlow(any(), any())).thenThrow(new EdcException("error"));
        manager.register(controller);

        var response = manager.initiate(dataRequest, policy);

        assertThat(response.succeeded()).isFalse();
        assertThat(response.getFailure().status()).isEqualTo(FATAL_ERROR);
        assertThat(response.getFailureMessages()).hasSize(1).first().matches(message -> message.contains("error"));
    }
}
