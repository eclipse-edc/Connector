/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.command.handlers;

import org.eclipse.edc.connector.controlplane.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyDataFlowErroredCommand;
import org.eclipse.edc.spi.command.CommandFailure;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class NotifyDataFlowErroredCommandHandlerTest {

    private final TransferProcessStore store = mock();
    private final TransferProcessListener listener = mock();
    private final NotifyDataFlowErroredCommandHandler handler = createHandler();

    @Test
    void verifyCorrectType() {
        assertThat(handler.getType()).isEqualTo(NotifyDataFlowErroredCommand.class);
    }

    @Test
    void shouldNotifyListeners_withoutChangingTheTransferProcess() {
        var transferProcess = TransferProcess.Builder.newInstance().id("test-id").state(STARTED.code()).build();
        when(store.findById("test-id")).thenReturn(transferProcess);

        var result = handler.handle(new NotifyDataFlowErroredCommand("test-id", "data-plane error"));

        assertThat(result.succeeded()).isTrue();
        verify(listener).dataFlowErrored(transferProcess, "data-plane error");
        assertThat(transferProcess.getState()).isEqualTo(STARTED.code());
        verify(store).findById("test-id");
        verifyNoMoreInteractions(store);
    }

    @Test
    void shouldReturnNotFound_whenTransferProcessDoesNotExist() {
        when(store.findById("test-id")).thenReturn(null);

        var result = handler.handle(new NotifyDataFlowErroredCommand("test-id", "data-plane error"));

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).isEqualTo(CommandFailure.Reason.NOT_FOUND);
        verify(listener, never()).dataFlowErrored(any(), any());
    }

    private NotifyDataFlowErroredCommandHandler createHandler() {
        var observable = new TransferProcessObservableImpl();
        observable.registerListener(listener);
        return new NotifyDataFlowErroredCommandHandler(store, observable);
    }
}
