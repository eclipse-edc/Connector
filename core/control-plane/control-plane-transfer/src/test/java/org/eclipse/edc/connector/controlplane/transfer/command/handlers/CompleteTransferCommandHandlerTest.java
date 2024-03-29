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

package org.eclipse.edc.connector.controlplane.transfer.command.handlers;

import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.CompleteTransferCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.mockito.Mockito.mock;

class CompleteTransferCommandHandlerTest {

    private final CompleteTransferCommandHandler handler = new CompleteTransferCommandHandler(mock(TransferProcessStore.class));

    @Test
    void shouldModify_whenItIsCompletable() {
        var command = new CompleteTransferCommand("test-id");
        var entity = TransferProcess.Builder.newInstance().state(STARTED.code()).build();

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(COMPLETING.code());
        assertThat(entity.getErrorDetail()).isNull();
    }

    @Test
    void shouldNotModify_whenItIsNotCompletable() {
        var command = new CompleteTransferCommand("test-id");
        var entity = TransferProcess.Builder.newInstance().state(TERMINATED.code()).build();

        var result = handler.modify(entity, command);

        assertThat(result).isFalse();
        assertThat(entity.getState()).isEqualTo(TERMINATED.code());
    }

    @Test
    void verifyCorrectType() {
        assertThat(handler.getType()).isEqualTo(CompleteTransferCommand.class);
    }
}
