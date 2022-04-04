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

package org.eclipse.dataspaceconnector.transfer.core.command.handlers;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.CancelTransferCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CancelTransferCommandHandlerTest {

    private CancelTransferCommandHandler handler;
    private TransferProcessStore storeMock;

    @BeforeEach
    void setUp() {
        storeMock = mock(TransferProcessStore.class);
        handler = new CancelTransferCommandHandler(storeMock);
    }

    @Test
    void handle() {
        var cmd = new CancelTransferCommand("test-id");
        var tp = TransferProcess.Builder.newInstance().id("test-id").state(TransferProcessStates.IN_PROGRESS.code())
                .type(TransferProcess.Type.CONSUMER).build();

        when(storeMock.find(anyString())).thenReturn(tp);
        handler.handle(cmd);

        assertThat(tp.getState()).isEqualTo(TransferProcessStates.CANCELLED.code());
        assertThat(tp.getErrorDetail()).isNull();

        verify(storeMock).find(anyString());
        verify(storeMock).update(tp);
        verifyNoMoreInteractions(storeMock);

    }

    @ParameterizedTest
    @EnumSource(value = TransferProcessStates.class, names = { "COMPLETED", "ENDED", "ERROR", "CANCELLED" })
    void handle_illegalState(TransferProcessStates targetState) {
        var tp = TransferProcess.Builder.newInstance().id("test-id").state(targetState.code())
                .type(TransferProcess.Type.CONSUMER).build();
        var cmd = new CancelTransferCommand("test-id");

        when(storeMock.find(anyString())).thenReturn(tp);
        handler.handle(cmd);

        verify(storeMock).find(anyString());
        verifyNoMoreInteractions(storeMock);
    }


    @Test
    void handle_notFound() {
        var cmd = new CancelTransferCommand("test-id");

        when(storeMock.find(anyString())).thenReturn(null);
        assertThatThrownBy(() -> handler.handle(cmd)).isInstanceOf(EdcException.class).hasMessageStartingWith("Could not find TransferProcess with ID [test-id]");
    }

    @Test
    void verifyCorrectType() {
        assertThat(handler.getType()).isEqualTo(CancelTransferCommand.class);
    }
}