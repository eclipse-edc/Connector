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

package org.eclipse.edc.connector.controlplane.transfer.command.handlers;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.DeprovisionRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.mockito.Mockito.mock;

class DeprovisionRequestCommandHandlerTest {

    private final DeprovisionRequestCommandHandler handler = new DeprovisionRequestCommandHandler(mock());

    @Test
    void shouldModify_whenTransferProcessIsDeprovisionable() {
        var entity = TransferProcess.Builder.newInstance().state(TERMINATED.code()).build();
        var command = new DeprovisionRequest("processId");

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(DEPROVISIONING.code());
    }

    @Test
    void shouldNotModify_whenTransferProcessIsNotDeprovisionable() {
        var entity = TransferProcess.Builder.newInstance().state(STARTED.code()).build();
        var command = new DeprovisionRequest("processId");

        var result = handler.modify(entity, command);

        assertThat(result).isFalse();
        assertThat(entity.getState()).isEqualTo(STARTED.code());
    }
}
