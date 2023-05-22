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

package org.eclipse.edc.connector.transfer.command.handlers;

import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.command.DeprovisionRequest;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeprovisionCommandHandlerTest {

    private TransferProcessStore storeMock;
    private DeprovisionRequestHandler handler;

    @BeforeEach
    void setUp() {
        storeMock = mock(TransferProcessStore.class);
        handler = new DeprovisionRequestHandler(storeMock);
    }

    @Test
    void handle() {
        var cmd = new DeprovisionRequest("test-id");
        var tp = TransferProcess.Builder.newInstance().id("test-id").state(TransferProcessStates.COMPLETED.code())
                .updatedAt(124123) //some invalid time
                .type(TransferProcess.Type.CONSUMER)
                .build();
        var originalDate = tp.getUpdatedAt();

        when(storeMock.findById(anyString())).thenReturn(tp);
        handler.handle(cmd);

        assertThat(tp.getState()).isEqualTo(TransferProcessStates.DEPROVISIONING.code());
        assertThat(tp.getErrorDetail()).isNull();
        assertThat(tp.getUpdatedAt()).isNotEqualTo(originalDate);
    }

    @Test
    void handle_notFound() {
        var cmd = new DeprovisionRequest("test-id");

        when(storeMock.findById(anyString())).thenReturn(null);
        assertThatThrownBy(() -> handler.handle(cmd)).isInstanceOf(EdcException.class).hasMessage("Could not find TransferProcess with ID [test-id]");
    }

    @Test
    void handle_transitionNotAllowed() {
        var cmd = new DeprovisionRequest("test-id");
        var tp = TransferProcess.Builder.newInstance().id("test-id").state(STARTED.code())
                .type(TransferProcess.Type.CONSUMER).build();
        var originalDate = tp.getUpdatedAt();

        when(storeMock.findById(anyString())).thenReturn(tp);
        assertThatThrownBy(() -> handler.handle(cmd)).isInstanceOf(IllegalStateException.class).hasMessage("Cannot transition from state STARTED to DEPROVISIONING");
        assertThat(tp.getUpdatedAt()).isEqualTo(originalDate);
    }

    @Test
    void getType() {
        assertThat(handler.getType()).isEqualTo(DeprovisionRequest.class);
    }
}
