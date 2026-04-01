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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.command.handlers;

import org.eclipse.edc.connector.controlplane.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.SuspendTransferCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SuspendTransferCommandHandlerTest {

    private final TransferProcessObservable observable = new TransferProcessObservableImpl();
    private final TransferProcessStore store = mock();
    private final SuspendTransferCommandHandler handler = new SuspendTransferCommandHandler(store, observable);

    @Test
    void verifyCorrectType() {
        assertThat(handler.getType()).isEqualTo(SuspendTransferCommand.class);
    }

    @Test
    void shouldModify_ifItCanBeSuspended() {
        var command = new SuspendTransferCommand("test-id", "a reason");
        var entity = TransferProcess.Builder.newInstance().state(STARTED.code()).build();

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(SUSPENDING.code());
        assertThat(entity.getErrorDetail()).isEqualTo("a reason");
    }

    @Test
    void shouldNotModify_ifItCannotBeSuspended() {
        var command = new SuspendTransferCommand("test-id", "a reason");
        var entity = TransferProcess.Builder.newInstance().state(COMPLETED.code()).build();

        var result = handler.modify(entity, command);

        assertThat(result).isFalse();
        assertThat(entity.getState()).isEqualTo(COMPLETED.code());
        assertThat(entity.getErrorDetail()).isNull();
    }

    @Test
    void postAction_shouldCallSuspending() {
        var command = new SuspendTransferCommand("test-id", "a reason");
        var entity = TransferProcess.Builder.newInstance().state(STARTED.code()).build();
        var listener = mock(TransferProcessListener.class);
        observable.registerListener(listener);

        handler.postActions(entity, command);

        verify(listener).suspending(entity);
    }
}
