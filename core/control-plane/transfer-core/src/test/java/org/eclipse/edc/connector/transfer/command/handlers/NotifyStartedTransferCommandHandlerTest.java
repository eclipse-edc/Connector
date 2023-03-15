/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.command.handlers;

import org.eclipse.edc.connector.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.command.NotifyStartedTransferCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotifyStartedTransferCommandHandlerTest {

    private final TransferProcessStore store = mock(TransferProcessStore.class);
    private final TransferProcessListener listener = mock(TransferProcessListener.class);

    private NotifyStartedTransferCommandHandler handler;

    @BeforeEach
    void setup() {
        var observable = new TransferProcessObservableImpl();
        observable.registerListener(listener);
        handler = new NotifyStartedTransferCommandHandler(store, observable);
    }

    @Test
    void type() {
        assertThat(handler.getType()).isEqualTo(NotifyStartedTransferCommand.class);
    }

    @Test
    void shouldTransitStateToStarted() {
        var process = TransferProcess.Builder.newInstance().id("processId").type(CONSUMER).state(REQUESTED.code()).build();
        when(store.find(process.getId())).thenReturn(process);

        handler.handle(new NotifyStartedTransferCommand("processId"));

        verify(store).save(argThat(p -> p.currentStateIsOneOf(STARTED) && p.getId().equals("processId")));
        verify(listener).started(isA(TransferProcess.class));
    }

    @Test
    void shouldNotTransitToStarted_whenItInAnInvalidState() {
        var process = TransferProcess.Builder.newInstance().id("processId").type(CONSUMER).state(COMPLETED.code()).build();
        when(store.find(process.getId())).thenReturn(process);

        handler.handle(new NotifyStartedTransferCommand("processId"));

        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }
}
