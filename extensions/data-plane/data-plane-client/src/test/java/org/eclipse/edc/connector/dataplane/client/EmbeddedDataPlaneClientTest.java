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

import org.eclipse.edc.connector.dataplane.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmbeddedDataPlaneClientTest {

    private DataPlaneManager dataPlaneManagerMock;
    private DataPlaneClient client;

    @BeforeEach
    public void setUp() {
        dataPlaneManagerMock = mock(DataPlaneManager.class);
        client = new EmbeddedDataPlaneClient(dataPlaneManagerMock);
    }

    @Test
    void verifyDataPlaneManagerMandatory() {
        assertThatNullPointerException().isThrownBy(() -> new EmbeddedDataPlaneClient(null));
    }

    @Test
    void verifyReturnFailedResultIfValidationFailure() {
        var errorMsg = "error";
        var request = createDataFlowRequest();
        when(dataPlaneManagerMock.validate(any())).thenReturn(Result.failure(errorMsg));
        doNothing().when(dataPlaneManagerMock).initiate(any());

        var result = client.transfer(request);

        verify(dataPlaneManagerMock).validate(request);
        verify(dataPlaneManagerMock, never()).initiate(any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .hasSize(1)
                .allSatisfy(s -> assertThat(s).contains(errorMsg));
    }

    @Test
    void verifyTransferSuccess() {
        var request = createDataFlowRequest();
        when(dataPlaneManagerMock.validate(any())).thenReturn(Result.success(true));
        doNothing().when(dataPlaneManagerMock).initiate(any());

        var result = client.transfer(request);

        verify(dataPlaneManagerMock).validate(request);
        verify(dataPlaneManagerMock).initiate(request);

        assertThat(result.succeeded()).isTrue();
    }

    private static DataFlowRequest createDataFlowRequest() {
        return DataFlowRequest.Builder.newInstance()
                .trackable(true)
                .id("123")
                .processId("456")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("test").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("test").build())
                .build();
    }
}
