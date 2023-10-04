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

package org.eclipse.edc.connector.transfer.flow;

import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DataFlowManagerImplTest {

    private final DataFlowManagerImpl manager = new DataFlowManagerImpl();

    @Test
    void initiate_shouldInitiateFlowOnCorrectController() {
        var controller = mock(DataFlowController.class);
        var dataRequest = DataRequest.Builder.newInstance().destinationType("test-dest-type").build();
        var policy = Policy.Builder.newInstance().build();
        var dataAddress = DataAddress.Builder.newInstance().type("test-type").build();
        var transferProcess = TransferProcess.Builder.newInstance().dataRequest(dataRequest).contentDataAddress(dataAddress).build();

        when(controller.canHandle(any())).thenReturn(true);
        when(controller.initiateFlow(any(), any())).thenReturn(StatusResult.success(DataFlowResponse.Builder.newInstance().build()));
        manager.register(controller);

        var response = manager.initiate(transferProcess, policy);

        assertThat(response.succeeded()).isTrue();
    }

    @Test
    void initiate_shouldReturnFatalError_whenNoControllerCanHandleTheRequest() {
        var controller = mock(DataFlowController.class);
        var dataRequest = DataRequest.Builder.newInstance().destinationType("test-dest-type").build();
        var dataAddress = DataAddress.Builder.newInstance().type("test-type").build();
        var policy = Policy.Builder.newInstance().build();
        var transferProcess = TransferProcess.Builder.newInstance().dataRequest(dataRequest).contentDataAddress(dataAddress).build();

        when(controller.canHandle(any())).thenReturn(false);
        manager.register(controller);

        var response = manager.initiate(transferProcess, policy);

        assertThat(response.succeeded()).isFalse();
        assertThat(response.getFailure().status()).isEqualTo(FATAL_ERROR);
    }

    @Test
    void initiate_shouldCatchExceptionsAndReturnFatalError() {
        var controller = mock(DataFlowController.class);
        var dataRequest = DataRequest.Builder.newInstance().destinationType("test-dest-type").build();
        var dataAddress = DataAddress.Builder.newInstance().type("test-type").build();
        var policy = Policy.Builder.newInstance().build();
        var transferProcess = TransferProcess.Builder.newInstance().dataRequest(dataRequest).contentDataAddress(dataAddress).build();

        var errorMsg = "Test Error Message";
        when(controller.canHandle(any())).thenReturn(true);
        when(controller.initiateFlow(any(), any())).thenThrow(new EdcException(errorMsg));
        manager.register(controller);

        var response = manager.initiate(transferProcess, policy);

        assertThat(response.succeeded()).isFalse();
        assertThat(response.getFailure().status()).isEqualTo(FATAL_ERROR);
        assertThat(response.getFailureMessages()).hasSize(1).first().matches(message -> message.contains(errorMsg));
    }

    @Test
    void initiate_shouldChooseHighestPriorityController() {
        var highPriority = createDataFlowController();
        var lowPriority = createDataFlowController();
        manager.register(1, lowPriority);
        manager.register(2, highPriority);

        manager.initiate(TransferProcess.Builder.newInstance().build(), Policy.Builder.newInstance().build());

        verify(highPriority).initiateFlow(any(), any());
        verifyNoInteractions(lowPriority);
    }

    @Test
    void terminate_shouldChooseControllerAndTerminate() {
        var controller = mock(DataFlowController.class);
        var dataRequest = DataRequest.Builder.newInstance().destinationType("test-dest-type").build();
        var dataAddress = DataAddress.Builder.newInstance().type("test-type").build();
        var transferProcess = TransferProcess.Builder.newInstance().dataRequest(dataRequest).contentDataAddress(dataAddress).build();

        when(controller.canHandle(any())).thenReturn(true);
        when(controller.terminate(any())).thenReturn(StatusResult.success());
        manager.register(controller);

        var result = manager.terminate(transferProcess);

        assertThat(result).isSucceeded();
        verify(controller).terminate(transferProcess);
    }

    private DataFlowController createDataFlowController() {
        var dataFlowController = mock(DataFlowController.class);
        when(dataFlowController.canHandle(any())).thenReturn(true);
        when(dataFlowController.initiateFlow(any(), any())).thenReturn(StatusResult.success(DataFlowResponse.Builder.newInstance().build()));
        return dataFlowController;
    }
}
