/*
 *  Copyright (c) 2022 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.dataplane.api.transfer;

import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneTransferControllerTest {
    private DataPlaneTransferController controller;
    private DataPlaneManager dataPlaneManager;

    @Test
    void verifyDispatch() {
        when(dataPlaneManager.validate(isA(DataFlowRequest.class))).thenReturn(Result.success(true));

        assertThat(controller.initiateRequest(createRequest()).getStatusInfo()).isEqualTo(OK);

        verify(dataPlaneManager, times(1)).initiateTransfer(isA(DataFlowRequest.class));
    }

    @Test
    void verifyNotValid() {
        when(dataPlaneManager.validate(isA(DataFlowRequest.class))).thenReturn(Result.failure("invalid"));

        assertThat(controller.initiateRequest(createRequest()).getStatusInfo()).isEqualTo(BAD_REQUEST);
    }

    @BeforeEach
    void setUp() {
        dataPlaneManager = mock(DataPlaneManager.class);
        controller = new DataPlaneTransferController(dataPlaneManager);
    }

    private DataFlowRequest createRequest() {
        return DataFlowRequest.Builder.newInstance()
                .id("1").processId("1")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("foo").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("foo").build())
                .build();
    }
}
