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

package org.eclipse.edc.connector.controlplane.transfer.flow;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DataFlowManagerImplTest {

    private final DataFlowManagerImpl manager = new DataFlowManagerImpl(mock());

    @Nested
    class Initiate {
        @Test
        void shouldInitiateFlowOnCorrectController() {
            var controller = mock(DataFlowController.class);
            var policy = Policy.Builder.newInstance().build();
            var dataAddress = DataAddress.Builder.newInstance().type("test-type").build();
            var transferProcess = TransferProcess.Builder.newInstance()
                    .dataDestination(DataAddress.Builder.newInstance().type("test-dest-type").build())
                    .contentDataAddress(dataAddress).build();

            when(controller.canHandle(any())).thenReturn(true);
            when(controller.start(any(), any())).thenReturn(StatusResult.success(DataFlowResponse.Builder.newInstance().build()));
            manager.register(controller);

            var response = manager.start(transferProcess, policy);

            assertThat(response.succeeded()).isTrue();
        }

        @Test
        void shouldReturnFatalError_whenNoControllerCanHandleTheRequest() {
            var controller = mock(DataFlowController.class);
            var dataDestination = DataAddress.Builder.newInstance().type("test-dest-type").build();
            var dataAddress = DataAddress.Builder.newInstance().type("test-type").build();
            var policy = Policy.Builder.newInstance().build();
            var transferProcess = TransferProcess.Builder.newInstance().dataDestination(dataDestination).contentDataAddress(dataAddress).build();

            when(controller.canHandle(any())).thenReturn(false);
            manager.register(controller);

            var response = manager.start(transferProcess, policy);

            assertThat(response.succeeded()).isFalse();
            assertThat(response.getFailure().status()).isEqualTo(FATAL_ERROR);
        }

        @Test
        void shouldCatchExceptionsAndReturnFatalError() {
            var controller = mock(DataFlowController.class);
            var dataDestination = DataAddress.Builder.newInstance().type("test-dest-type").build();
            var dataAddress = DataAddress.Builder.newInstance().type("test-type").build();
            var policy = Policy.Builder.newInstance().build();
            var transferProcess = TransferProcess.Builder.newInstance().dataDestination(dataDestination).contentDataAddress(dataAddress).build();

            var errorMsg = "Test Error Message";
            when(controller.canHandle(any())).thenReturn(true);
            when(controller.start(any(), any())).thenThrow(new EdcException(errorMsg));
            manager.register(controller);

            var response = manager.start(transferProcess, policy);

            assertThat(response.succeeded()).isFalse();
            assertThat(response.getFailure().status()).isEqualTo(FATAL_ERROR);
            assertThat(response.getFailureMessages()).hasSize(1).first().matches(message -> message.contains(errorMsg));
        }

        @Test
        void shouldChooseHighestPriorityController() {
            var highPriority = createDataFlowController();
            var lowPriority = createDataFlowController();
            manager.register(1, lowPriority);
            manager.register(2, highPriority);

            manager.start(TransferProcess.Builder.newInstance().build(), Policy.Builder.newInstance().build());

            verify(highPriority).start(any(), any());
            verifyNoInteractions(lowPriority);
        }

        private DataFlowController createDataFlowController() {
            var dataFlowController = mock(DataFlowController.class);
            when(dataFlowController.canHandle(any())).thenReturn(true);
            when(dataFlowController.start(any(), any())).thenReturn(StatusResult.success(DataFlowResponse.Builder.newInstance().build()));
            return dataFlowController;
        }
    }

    @Nested
    class Provision {
        @Test
        void shouldChooseControllerAndProvision() {
            var controller = mock(DataFlowController.class);
            var dataDestination = DataAddress.Builder.newInstance().type("test-dest-type").build();
            var dataAddress = DataAddress.Builder.newInstance().type("test-type").build();
            var transferProcess = TransferProcess.Builder.newInstance().dataDestination(dataDestination).contentDataAddress(dataAddress).build();
            var policy = Policy.Builder.newInstance().build();

            when(controller.canHandle(any())).thenReturn(true);
            when(controller.prepare(any(), any())).thenReturn(StatusResult.success(DataFlowResponse.Builder.newInstance().build()));
            manager.register(controller);

            var result = manager.prepare(transferProcess, policy);

            assertThat(result).isSucceeded();
            verify(controller).prepare(transferProcess, policy);
        }
    }

    @Nested
    class Suspend {
        @Test
        void shouldChooseControllerAndSuspend() {
            var controller = mock(DataFlowController.class);
            var dataDestination = DataAddress.Builder.newInstance().type("test-dest-type").build();
            var dataAddress = DataAddress.Builder.newInstance().type("test-type").build();
            var transferProcess = TransferProcess.Builder.newInstance().dataDestination(dataDestination).contentDataAddress(dataAddress).build();

            when(controller.canHandle(any())).thenReturn(true);
            when(controller.suspend(any())).thenReturn(StatusResult.success());
            manager.register(controller);

            var result = manager.suspend(transferProcess);

            assertThat(result).isSucceeded();
            verify(controller).suspend(transferProcess);
        }
    }

    @Nested
    class Terminate {
        @Test
        void shouldChooseControllerAndTerminate() {
            var controller = mock(DataFlowController.class);
            var dataDestination = DataAddress.Builder.newInstance().type("test-dest-type").build();
            var dataAddress = DataAddress.Builder.newInstance().type("test-type").build();
            var transferProcess = TransferProcess.Builder.newInstance().dataDestination(dataDestination).contentDataAddress(dataAddress).build();

            when(controller.canHandle(any())).thenReturn(true);
            when(controller.terminate(any())).thenReturn(StatusResult.success());
            manager.register(controller);

            var result = manager.terminate(transferProcess);

            assertThat(result).isSucceeded();
            verify(controller).terminate(transferProcess);
        }
    }

    @Nested
    class TransferTypesFor {
        @Test
        void shouldReturnTransferTypesFromControllers() {
            var controllerOne = mock(DataFlowController.class);
            when(controllerOne.transferTypesFor(any())).thenReturn(Set.of("Type1"));
            var controllerTwo = mock(DataFlowController.class);
            when(controllerTwo.transferTypesFor(any())).thenReturn(Set.of("Type2", "Type3"));
            manager.register(controllerOne);
            manager.register(controllerTwo);
            var asset = Asset.Builder.newInstance().build();


            var result = manager.transferTypesFor(asset);

            assertThat(result).containsExactlyInAnyOrder("Type1", "Type2", "Type3");
        }
    }

}
