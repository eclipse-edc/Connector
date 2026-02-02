/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.command.handlers;

import org.eclipse.edc.connector.controlplane.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyStartedCommand;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotifyStartedCommandHandlerTest {

    private final TransferProcessObservable observable = new TransferProcessObservableImpl();
    private final NotifyStartedCommandHandler handler = new NotifyStartedCommandHandler(mock(TransferProcessStore.class), observable);

    @Test
    void verifyCorrectType() {
        assertThat(handler.getType()).isEqualTo(NotifyStartedCommand.class);
    }

    @Test
    void shouldNotTransition_whenConsumer() {
        var newDestination = DataAddress.Builder.newInstance().type("new").build();
        var originalDestination = DataAddress.Builder.newInstance().type("original").build();
        var command = new NotifyStartedCommand("test-id", newDestination);
        var entity = TransferProcess.Builder.newInstance().state(STARTING.code()).type(CONSUMER).dataDestination(originalDestination).build();

        var result = handler.modify(entity, command);

        assertThat(result).isFalse();
        assertThat(entity.getState()).isEqualTo(STARTING.code());
        assertThat(entity.getDataDestination()).isSameAs(newDestination);
    }

    @Test
    void shouldTransitionToStarting_whenProvider() {
        var newDestination = DataAddress.Builder.newInstance().type("new").build();
        var originalDestination = DataAddress.Builder.newInstance().type("original").build();
        var command = new NotifyStartedCommand("test-id", newDestination);
        var entity = TransferProcess.Builder.newInstance().state(STARTING.code()).type(PROVIDER).dataDestination(originalDestination).build();

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(STARTING.code());
        assertThat(entity.getDataDestination()).isSameAs(newDestination);
    }

    @Test
    void shouldUpdateDestination_whenProvided() {
        var newDestination = DataAddress.Builder.newInstance().type("new").build();
        var originalDestination = DataAddress.Builder.newInstance().type("original").build();
        var command = new NotifyStartedCommand("test-id", newDestination);
        var entity = TransferProcess.Builder.newInstance().state(STARTING.code()).type(PROVIDER).dataDestination(originalDestination).build();

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        assertThat(entity.getDataDestination()).isSameAs(newDestination);
    }

    @Test
    void shouldNotUpdateDestination_whenItIsMissing() {
        var command = new NotifyStartedCommand("test-id", null);
        var originalDestination = DataAddress.Builder.newInstance().type("original").build();
        var entity = TransferProcess.Builder.newInstance().state(STARTING.code()).type(PROVIDER).dataDestination(originalDestination).build();

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(STARTING.code());
        assertThat(entity.getDataDestination()).isSameAs(originalDestination);
    }

    @Test
    void postAction_shouldCallStarted() {
        var dataAddress = DataAddress.Builder.newInstance().type("test").build();
        var command = new NotifyStartedCommand("test-id", null);
        var entity = TransferProcess.Builder.newInstance().state(STARTING.code()).contentDataAddress(dataAddress).build();
        var listener = mock(TransferProcessListener.class);
        observable.registerListener(listener);

        handler.postActions(entity, command);

        var captor = ArgumentCaptor.forClass(org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessStartedData.class);
        verify(listener).started(eq(entity), captor.capture());
        assertThat(captor.getValue().getDataAddress()).isSameAs(dataAddress);
    }
}
