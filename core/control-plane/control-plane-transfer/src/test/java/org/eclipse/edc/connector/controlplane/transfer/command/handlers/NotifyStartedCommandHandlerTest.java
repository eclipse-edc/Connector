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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyStartedCommand;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTUP_REQUESTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotifyStartedCommandHandlerTest {

    private final TransferProcessObservable observable = new TransferProcessObservableImpl();
    private final DataAddressStore dataAddressStore = mock();
    private final NotifyStartedCommandHandler handler = new NotifyStartedCommandHandler(mock(TransferProcessStore.class), observable, dataAddressStore);

    @Test
    void verifyCorrectType() {
        assertThat(handler.getType()).isEqualTo(NotifyStartedCommand.class);
    }

    @Test
    void shouldNotTransition_whenConsumer() {
        var dataAddress = DataAddress.Builder.newInstance().type("new").build();
        var command = new NotifyStartedCommand("test-id", dataAddress);
        var entity = TransferProcess.Builder.newInstance().state(STARTING.code()).type(CONSUMER).build();

        var result = handler.modify(entity, command);

        assertThat(result).isFalse();
        assertThat(entity.getState()).isEqualTo(STARTING.code());
    }

    @Test
    void shouldTransitionToStarting_whenProvider() {
        var dataAddress = DataAddress.Builder.newInstance().type("new").build();
        var command = new NotifyStartedCommand("test-id", dataAddress);
        var entity = TransferProcess.Builder.newInstance().state(STARTING.code()).type(PROVIDER).build();
        when(dataAddressStore.store(dataAddress, entity)).thenReturn(StoreResult.success());

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(STARTING.code());
        verify(dataAddressStore).store(dataAddress, entity);
    }

    @Test
    void shouldNotUpdateDestination_whenItIsMissing() {
        var command = new NotifyStartedCommand("test-id", null);
        var entity = TransferProcess.Builder.newInstance().state(STARTING.code()).type(PROVIDER).build();

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        assertThat(entity.getState()).isEqualTo(STARTING.code());
        verify(dataAddressStore, never()).store(any(), any());
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

    @Test
    void shouldNotTransition_whenDataAddressStoreOperationFails() {
        var dataAddress = DataAddress.Builder.newInstance().type("new").build();
        var command = new NotifyStartedCommand("test-id", dataAddress);
        var entity = TransferProcess.Builder.newInstance().state(STARTUP_REQUESTED.code()).type(PROVIDER).build();
        when(dataAddressStore.store(dataAddress, entity)).thenReturn(StoreResult.notFound("Failed to store"));

        var result = handler.modify(entity, command);

        assertThat(result).isFalse();
        verify(dataAddressStore).store(dataAddress, entity);
    }
}
