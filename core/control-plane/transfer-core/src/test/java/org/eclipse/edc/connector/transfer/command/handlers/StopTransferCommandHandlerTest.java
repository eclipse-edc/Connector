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
import org.eclipse.edc.connector.transfer.spi.types.command.StopTransferCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STOPPING;
import static org.mockito.Mockito.mock;

class StopTransferCommandHandlerTest {

    private final TransferProcessStore store = mock();
    private final StopTransferCommandHandler handler = new StopTransferCommandHandler(store);

    @Test
    void verifyCorrectType() {
        assertThat(handler.getType()).isEqualTo(StopTransferCommand.class);
    }

    @Test
    void shouldModify_ifItCanBeStopped() {
        var command = new StopTransferCommand("test-id", "a reason", COMPLETING);
        var entity = TransferProcess.Builder.newInstance().type(PROVIDER).state(STARTED.code()).build();

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(STOPPING.code());
        assertThat(entity.getErrorDetail()).isEqualTo("a reason");
        assertThat(entity.stoppingSubsequentState()).isEqualTo(COMPLETING);
    }

    @Test
    void shouldNotModify_ifItCannotBeStopped() {
        var command = new StopTransferCommand("test-id", "a reason", STARTING);
        var entity = TransferProcess.Builder.newInstance().type(PROVIDER).state(COMPLETED.code()).build();

        var result = handler.modify(entity, command);

        assertThat(result).isFalse();
        assertThat(entity.getState()).isEqualTo(COMPLETED.code());
        assertThat(entity.getErrorDetail()).isNull();
    }

    @Test
    void shouldNotModify_ifItIsConsumer() {
        var command = new StopTransferCommand("test-id", "a reason", COMPLETING);
        var entity = TransferProcess.Builder.newInstance().type(CONSUMER).state(STARTED.code()).build();

        var result = handler.modify(entity, command);

        assertThat(result).isFalse();
    }
}
