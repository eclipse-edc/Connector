/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.client;

import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmbeddedDataPlaneClientTest {

    private final DataPlaneManager dataPlaneManager = mock();
    private final DataPlaneClient client = new EmbeddedDataPlaneClient(dataPlaneManager);

    @Nested
    class Provision {

        @Test
        void shouldSucceed_whenFlowPreparedCorrectly() {
            var response = DataFlowResponseMessage.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("type").build()).build();
            var request = DataFlowProvisionMessage.Builder.newInstance()
                    .processId("456")
                    .destination(DataAddress.Builder.newInstance().type("test").build())
                    .build();
            when(dataPlaneManager.provision(any())).thenReturn(StatusResult.success(response));

            var result = client.prepare(request);

            assertThat(result).isSucceeded().isEqualTo(response);
        }

        @Test
        void shouldFail_whenPreparationFails() {
            var request = DataFlowProvisionMessage.Builder.newInstance()
                    .processId("456")
                    .destination(DataAddress.Builder.newInstance().type("test").build())
                    .build();
            when(dataPlaneManager.provision(any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR, "error"));

            var result = client.prepare(request);

            assertThat(result).isFailed();
        }
    }

    @Test
    void transfer_shouldSucceed_whenTransferInitiatedCorrectly() {
        var response = DataFlowResponseMessage.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("type").build()).build();
        var request = createDataFlowRequest();
        when(dataPlaneManager.validate(any())).thenReturn(Result.success());
        when(dataPlaneManager.start(any())).thenReturn(StatusResult.success(response));

        var result = client.start(request);

        verify(dataPlaneManager).validate(request);
        verify(dataPlaneManager).start(request);

        assertThat(result).isSucceeded().isEqualTo(response);
    }

    @Test
    void transfer_shouldReturnFailedResult_whenValidationFailure() {
        var errorMsg = "error";
        var request = createDataFlowRequest();
        when(dataPlaneManager.validate(any())).thenReturn(Result.failure(errorMsg));
        when(dataPlaneManager.start(any())).thenReturn(StatusResult.success(DataFlowResponseMessage.Builder.newInstance().build()));

        var result = client.start(request);

        verify(dataPlaneManager).validate(request);
        verify(dataPlaneManager, never()).start(any());

        assertThat(result).isFailed().messages().hasSize(1).allSatisfy(s -> assertThat(s).contains(errorMsg));
    }

    @Test
    void transfer_shouldReturnFailedResult_whenStartFailure() {
        var errorMsg = "error";
        var request = createDataFlowRequest();
        when(dataPlaneManager.validate(any())).thenReturn(Result.success());
        when(dataPlaneManager.start(any())).thenReturn(StatusResult.failure(ResponseStatus.ERROR_RETRY, errorMsg));

        var result = client.start(request);

        verify(dataPlaneManager).validate(request);
        verify(dataPlaneManager).start(request);

        assertThat(result).isFailed().messages().hasSize(1).allSatisfy(s -> assertThat(s).contains(errorMsg));
    }

    @Test
    void suspend_shouldProxyCallToManager() {
        when(dataPlaneManager.suspend(any())).thenReturn(StatusResult.success());

        var result = client.suspend("dataFlowId");

        assertThat(result).isSucceeded();
        verify(dataPlaneManager).suspend("dataFlowId");
    }

    @Test
    void terminate_shouldProxyCallToManager() {
        when(dataPlaneManager.terminate(any())).thenReturn(StatusResult.success());

        var result = client.terminate("dataFlowId");

        assertThat(result).isSucceeded();
        verify(dataPlaneManager).terminate("dataFlowId");
    }

    private DataFlowStartMessage createDataFlowRequest() {
        return DataFlowStartMessage.Builder.newInstance()
                .id("123")
                .processId("456")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("test").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("test").build())
                .build();
    }

}
