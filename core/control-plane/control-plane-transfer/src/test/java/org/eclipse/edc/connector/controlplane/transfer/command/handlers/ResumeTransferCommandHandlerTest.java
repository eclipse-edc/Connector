/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.ResumeTransferCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ResumeTransferCommandHandlerTest {

    private final TransferProcessObservable observable = new TransferProcessObservableImpl();
    private final TransferProcessStore store = mock();
    private final ResumeTransferCommandHandler handler = new ResumeTransferCommandHandler(store, observable);

    @Test
    void verifyCorrectType() {
        assertThat(handler.getType()).isEqualTo(ResumeTransferCommand.class);
    }

    @Test
    void shouldModify_ifItCanBeResumed() {
        var command = new ResumeTransferCommand("test-id");
        var entity = TransferProcess.Builder.newInstance().state(SUSPENDED.code()).build();

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(RESUMING.code());
    }

    @Test
    void postAction_shouldCallResumingRequested() {
        var command = new ResumeTransferCommand("test-id");
        var entity = TransferProcess.Builder.newInstance().state(SUSPENDED.code()).build();
        var listener = mock(TransferProcessListener.class);
        observable.registerListener(listener);

        handler.postActions(entity, command);

        verify(listener).resuming(entity);
    }

    @Test
    void shouldNotModify_ifItCannotBeResumed() {
        var command = new ResumeTransferCommand("test-id");
        var entity = TransferProcess.Builder.newInstance().state(COMPLETED.code()).build();

        var result = handler.modify(entity, command);

        assertThat(result).isFalse();
        assertThat(entity.getState()).isEqualTo(COMPLETED.code());
    }
}
