/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.command.handlers;

import org.eclipse.edc.connector.controlplane.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.CompleteProvisionCommand;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONING_REQUESTED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CompleteProvisionCommandHandlerTest {

    private final TransferProcessObservable observable = new TransferProcessObservableImpl();
    private final CompleteProvisionCommandHandler handler = new CompleteProvisionCommandHandler(mock(TransferProcessStore.class), observable);

    @Test
    void verifyCorrectType() {
        assertThat(handler.getType()).isEqualTo(CompleteProvisionCommand.class);
    }

    @Test
    void shouldUpdateDestination() {
        var newDestination = DataAddress.Builder.newInstance().type("new").build();
        var originalDestination = DataAddress.Builder.newInstance().type("original").build();
        var command = new CompleteProvisionCommand("test-id", newDestination);
        var entity = TransferProcess.Builder.newInstance().state(PROVISIONING_REQUESTED.code()).dataDestination(originalDestination).build();

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(PROVISIONED.code());
        assertThat(entity.getDataDestination()).isSameAs(newDestination);
    }

    @Test
    void shouldNotUpdateDestination_whenItIsMissing() {
        var command = new CompleteProvisionCommand("test-id", null);
        var originalDestination = DataAddress.Builder.newInstance().type("original").build();
        var entity = TransferProcess.Builder.newInstance().state(PROVISIONING_REQUESTED.code()).dataDestination(originalDestination).build();

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(PROVISIONED.code());
        assertThat(entity.getDataDestination()).isSameAs(originalDestination);
    }

    @Test
    void postAction_shouldCallProvisioned() {
        var command = new CompleteProvisionCommand("test-id", null);
        var entity = TransferProcess.Builder.newInstance().state(PROVISIONING_REQUESTED.code()).build();
        var listener = mock(TransferProcessListener.class);
        observable.registerListener(listener);

        handler.postActions(entity, command);

        verify(listener).provisioned(entity);
    }
}
