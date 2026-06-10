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

package org.eclipse.edc.connector.controlplane.callback.dispatcher;

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackClient;
import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackRegistry;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessCompleted;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class CallbackEventDispatcherTest {

    CallbackEventDispatcher dispatcher;
    CallbackClient callbackClient = mock(CallbackClient.class);
    CallbackRegistry callbackRegistry = mock(CallbackRegistry.class);
    Monitor monitor = mock(Monitor.class);

    @Test
    void verifyShouldNotDispatch() {
        dispatcher = new CallbackEventDispatcher(callbackClient, callbackRegistry, true, monitor);

        var event = TransferProcessCompleted.Builder.newInstance().transferProcessId("id").build();
        dispatcher.on(envelope(event));

        verifyNoInteractions(callbackClient);
    }

    @Test
    void verifyShouldDispatch_WhenCallbacksMatchedOnRegistry() {
        dispatcher = new CallbackEventDispatcher(callbackClient, callbackRegistry, true, monitor);
        var event = TransferProcessCompleted.Builder.newInstance().transferProcessId("id").build();
        var callbacks = List.of(CallbackAddress.Builder.newInstance()
                .uri("http://test")
                .events(Set.of("transfer.process.completed"))
                .transactional(true)
                .build());

        when(callbackRegistry.resolve(event.name())).thenReturn(callbacks);

        dispatcher.on(envelope(event));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventEnvelope<TransferProcessCompleted>> captor = ArgumentCaptor.forClass(EventEnvelope.class);

        verify(callbackClient).dispatch(any(), captor.capture());

        assertThat(captor.getValue().getPayload())
                .usingRecursiveComparison()
                .isEqualTo(event);
    }

    @Test
    void verifyDispatchShouldThrowException() {
        dispatcher = new CallbackEventDispatcher(callbackClient, callbackRegistry, true, monitor);

        doThrow(new RuntimeException("Test")).when(callbackClient).dispatch(any(), any());

        var callback = CallbackAddress.Builder.newInstance()
                .uri("http://test")
                .events(Set.of("transfer.process.completed"))
                .transactional(true)
                .build();

        var event = TransferProcessCompleted.Builder.newInstance()
                .transferProcessId("id")
                .callbackAddresses(List.of(callback))
                .build();

        assertThatThrownBy(() -> dispatcher.on(envelope(event))).isInstanceOf(EdcException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void verifyShouldDispatchWithSameTransactionalConfiguration(boolean transactional) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventEnvelope<TransferProcessCompleted>> captor = ArgumentCaptor.forClass(EventEnvelope.class);

        dispatcher = new CallbackEventDispatcher(callbackClient, callbackRegistry, transactional, monitor);

        var callback = CallbackAddress.Builder.newInstance()
                .uri("http://test")
                .events(Set.of("transfer.process.completed"))
                .transactional(transactional)
                .build();

        var event = TransferProcessCompleted.Builder.newInstance()
                .transferProcessId("id")
                .callbackAddresses(List.of(callback))
                .build();

        dispatcher.on(envelope(event));

        verify(callbackClient).dispatch(any(), captor.capture());

        assertThat(captor.getValue().getPayload().getCallbackAddresses())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(callback);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void verifyShouldNotDispatchWithDifferentTransactionalConfiguration(boolean transactional) {
        dispatcher = new CallbackEventDispatcher(callbackClient, callbackRegistry, transactional, monitor);

        var callback = CallbackAddress.Builder.newInstance()
                .uri("http://test")
                .events(Set.of("transfer.process.completed"))
                .transactional(!transactional)
                .build();

        var event = TransferProcessCompleted.Builder.newInstance()
                .transferProcessId("id")
                .callbackAddresses(List.of(callback))
                .build();

        dispatcher.on(envelope(event));

        verifyNoInteractions(callbackClient);
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> EventEnvelope<T> envelope(T event) {
        return EventEnvelope.Builder.newInstance().id("test").at(10).payload(event).build();
    }
}
