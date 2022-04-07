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

package org.eclipse.dataspaceconnector.transfer.dataplane.client;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.transfer.dataplane.TestFixtures.createDataFlowRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmbeddedDataPlaneTransferClientTest {

    private static final Faker FAKER = new Faker();

    private DataPlaneManager dataPlaneManagerMock;
    private DataPlaneTransferClient client;

    @BeforeEach
    public void setUp() {
        dataPlaneManagerMock = mock(DataPlaneManager.class);
        client = new EmbeddedDataPlaneTransferClient(dataPlaneManagerMock);
    }

    @Test
    void validationFailure_shouldReturnFailedResult() {
        var errorMsg = FAKER.internet().uuid();
        var request = createDataFlowRequest();
        when(dataPlaneManagerMock.validate(any())).thenReturn(Result.failure(errorMsg));
        doNothing().when(dataPlaneManagerMock).initiateTransfer(any());

        var result = client.transfer(request);

        verify(dataPlaneManagerMock, times(1)).validate(request);
        verify(dataPlaneManagerMock, never()).initiateTransfer(any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .hasSize(1)
                .allSatisfy(s -> assertThat(s).contains(errorMsg));
    }

    @Test
    void transferSuccess() {
        var request = createDataFlowRequest();
        when(dataPlaneManagerMock.validate(any())).thenReturn(Result.success(true));
        doNothing().when(dataPlaneManagerMock).initiateTransfer(any());

        var result = client.transfer(request);

        verify(dataPlaneManagerMock, times(1)).validate(request);
        verify(dataPlaneManagerMock, times(1)).initiateTransfer(request);

        assertThat(result.succeeded()).isTrue();
    }
}
