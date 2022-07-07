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
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessListener;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessObservable;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.CancelTransferCommand;
import org.eclipse.dataspaceconnector.transfer.core.observe.TransferProcessObservableImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CancelTransferCommandHandlerTest {

    private final TransferProcessStore store = mock(TransferProcessStore.class);
    private final TransferProcessObservable observable = new TransferProcessObservableImpl();
    private final TransferProcessListener listener = mock(TransferProcessListener.class);
    private CancelTransferCommandHandler handler;

    @BeforeEach
    void setUp() {
        observable.registerListener(listener);
        handler = new CancelTransferCommandHandler(store, observable);
    }

    @Test
    void handle() {
        var cmd = new CancelTransferCommand("test-id");
        var tp = TransferProcess.Builder.newInstance().id("test-id").state(TransferProcessStates.IN_PROGRESS.code())
                .type(TransferProcess.Type.CONSUMER).build();

        when(store.find(anyString())).thenReturn(tp);
        handler.handle(cmd);

        assertThat(tp.getState()).isEqualTo(TransferProcessStates.CANCELLED.code());
        assertThat(tp.getErrorDetail()).isNull();

        verify(store).find(anyString());
        verify(store).update(tp);
        verifyNoMoreInteractions(store);
        verify(listener).cancelled(tp);
    }

    @ParameterizedTest
    @EnumSource(value = TransferProcessStates.class, names = { "COMPLETED", "ENDED", "ERROR", "CANCELLED" })
    void handle_illegalState(TransferProcessStates targetState) {
        var tp = TransferProcess.Builder.newInstance().id("test-id").state(targetState.code())
                .type(TransferProcess.Type.CONSUMER).build();
        var cmd = new CancelTransferCommand("test-id");

        when(store.find(anyString())).thenReturn(tp);
        handler.handle(cmd);

        verify(store).find(anyString());
        verifyNoMoreInteractions(store);
        verifyNoInteractions(listener);
    }

    @Test
    void handle_notFound() {
        var cmd = new CancelTransferCommand("test-id");

        when(store.find(anyString())).thenReturn(null);
        assertThatThrownBy(() -> handler.handle(cmd)).isInstanceOf(EdcException.class).hasMessageStartingWith("Could not find TransferProcess with ID [test-id]");
        verifyNoInteractions(listener);
    }

    @Test
    void verifyCorrectType() {
        assertThat(handler.getType()).isEqualTo(CancelTransferCommand.class);
    }
}