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
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.transfer.command.CompleteTransferCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CompleteTransferCommandHandlerTest {

    private final TransferProcessStore store = mock(TransferProcessStore.class);

    private final CompleteTransferCommandHandler handler = new CompleteTransferCommandHandler(store);

    @Test
    void handle() {
        var command = new CompleteTransferCommand("test-id");
        var tp = TransferProcess.Builder.newInstance().id("test-id").state(STARTED.code())
                .updatedAt(124123) //some invalid time
                .type(TransferProcess.Type.CONSUMER).build();
        var originalDate = tp.getUpdatedAt();
        when(store.find(anyString())).thenReturn(tp);

        handler.handle(command);

        assertThat(tp.getState()).isEqualTo(COMPLETING.code());
        assertThat(tp.getErrorDetail()).isNull();
        assertThat(tp.getUpdatedAt()).isNotEqualTo(originalDate);
        verify(store).find(anyString());
        verify(store).save(tp);
        verifyNoMoreInteractions(store);
    }

    @ParameterizedTest
    @EnumSource(value = TransferProcessStates.class, names = { "COMPLETED", "TERMINATED" })
    void handle_illegalState(TransferProcessStates targetState) {
        var tp = TransferProcess.Builder.newInstance().id("test-id").state(targetState.code())
                .type(TransferProcess.Type.CONSUMER).build();
        var originalDate = tp.getUpdatedAt();
        var command = new CompleteTransferCommand("test-id");
        when(store.find(anyString())).thenReturn(tp);

        handler.handle(command);

        assertThat(tp.getUpdatedAt()).isEqualTo(originalDate);
        verify(store).find(anyString());
        verifyNoMoreInteractions(store);
    }

    @Test
    void handle_notFound() {
        var command = new CompleteTransferCommand("test-id");
        when(store.find(anyString())).thenReturn(null);

        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(EdcException.class).hasMessageStartingWith("Could not find TransferProcess with ID [test-id]");
    }

    @Test
    void verifyCorrectType() {
        assertThat(handler.getType()).isEqualTo(CompleteTransferCommand.class);
    }
}
