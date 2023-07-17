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

package org.eclipse.edc.connector.transfer.command.handlers;

import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.command.TerminateTransferCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.mockito.Mockito.mock;

class TerminateTransferCommandHandlerTest {

    private final TransferProcessStore store = mock(TransferProcessStore.class);
    private TerminateTransferCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TerminateTransferCommandHandler(store);
    }

    @Test
    void verifyCorrectType() {
        assertThat(handler.getType()).isEqualTo(TerminateTransferCommand.class);
    }

    @Test
    void shouldModify_ifItCanBeTerminated() {
        var command = new TerminateTransferCommand("test-id", "a reason");
        var entity = TransferProcess.Builder.newInstance().state(STARTED.code()).build();

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(TERMINATING.code());
        assertThat(entity.getErrorDetail()).isEqualTo("a reason");
    }

    @Test
    void shouldNotModify_ifItCannotBeTerminated() {
        var command = new TerminateTransferCommand("test-id", "a reason");
        var entity = TransferProcess.Builder.newInstance().state(COMPLETED.code()).build();

        var result = handler.modify(entity, command);

        assertThat(result).isFalse();
        assertThat(entity.getState()).isEqualTo(COMPLETED.code());
        assertThat(entity.getErrorDetail()).isNull();
    }
}
