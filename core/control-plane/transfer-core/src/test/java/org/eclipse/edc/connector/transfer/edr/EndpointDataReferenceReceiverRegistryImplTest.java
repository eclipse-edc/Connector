/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.edr;

import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiver;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EndpointDataReferenceReceiverRegistryImplTest {

    private final EndpointDataReferenceReceiver receiver1 = mock(EndpointDataReferenceReceiver.class);
    private final EndpointDataReferenceReceiver receiver2 = mock(EndpointDataReferenceReceiver.class);
    private final TypeTransformerRegistry typeTransformerRegistry = mock(TypeTransformerRegistry.class);

    private final EndpointDataReferenceReceiverRegistryImpl registry = new EndpointDataReferenceReceiverRegistryImpl(typeTransformerRegistry);
    
    @Test
    void onEvent_success() {
        var address = DataAddress.Builder.newInstance().type("test").build();
        registry.registerReceiver(receiver1);
        registry.registerReceiver(receiver2);

        var edr = createEndpointDataReference();

        var captor = ArgumentCaptor.forClass(EndpointDataReference.class);
        when(typeTransformerRegistry.transform(address, EndpointDataReference.class)).thenReturn(Result.success(edr));
        when(receiver1.send(captor.capture())).thenReturn(CompletableFuture.completedFuture(Result.success()));
        when(receiver2.send(captor.capture())).thenReturn(CompletableFuture.completedFuture(Result.success()));

        var envelope = EventEnvelope.Builder.newInstance()
                .at(10)
                .id("id")
                .payload(TransferProcessStarted.Builder.newInstance()
                        .transferProcessId("id")
                        .dataAddress(address)
                        .build())
                .build();

        registry.on(envelope);

        assertThat(captor.getAllValues()).allSatisfy(sent -> assertThat(sent).usingRecursiveComparison().isEqualTo(edr));
    }

    @Test
    void onEvent_failsBecauseDataAddressCannotBeTransformedToEdr_shouldReturnFailedResult() {
        var address = DataAddress.Builder.newInstance().type("test").build();
        registry.registerReceiver(receiver1);
        registry.registerReceiver(receiver2);
        var errorMsg = "Test error message";

        when(typeTransformerRegistry.transform(address, EndpointDataReference.class)).thenReturn(Result.failure(errorMsg));

        var envelope = envelope(startedEvent(address));

        assertThatThrownBy(() -> registry.on(envelope)).isInstanceOf(EdcException.class).hasMessageContaining(errorMsg);

        verifyNoInteractions(receiver1, receiver2);
    }

    @Test
    void onEvent_failsBecauseReceiverReturnsFailedResult_shouldReturnFailedResult() {
        var address = dataAddress();
        registry.registerReceiver(receiver1);
        registry.registerReceiver(receiver2);

        var edr = createEndpointDataReference();
        var errorMsg = "Test error message";

        when(typeTransformerRegistry.transform(address, EndpointDataReference.class)).thenReturn(Result.success(edr));
        when(receiver1.send(any())).thenReturn(CompletableFuture.completedFuture(Result.success()));
        when(receiver2.send(any())).thenReturn(CompletableFuture.completedFuture(Result.failure(errorMsg)));

        var envelope = envelope(startedEvent(address));

        assertThatThrownBy(() -> registry.on(envelope)).isInstanceOf(EdcException.class).hasMessage(errorMsg);

        verify(receiver1).send(any());
        verify(receiver2).send(any());

    }

    private TransferProcessStarted startedEvent(DataAddress address) {
        return TransferProcessStarted.Builder.newInstance()
                .transferProcessId("id")
                .dataAddress(address)
                .build();
    }

    private <E extends Event> EventEnvelope<E> envelope(E e) {
        return EventEnvelope.Builder.newInstance()
                .at(10)
                .id("id")
                .payload(e)
                .build();
    }

    private static DataAddress dataAddress() {
        return DataAddress.Builder.newInstance().type("test").build();
    }

    private static EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .endpoint("test.endpoint.url")
                .authKey("test-authkey")
                .authCode(UUID.randomUUID().toString())
                .id(UUID.randomUUID().toString())
                .properties(Map.of("test-key", UUID.randomUUID().toString()))
                .build();
    }
}
