/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.transfer.spi.event;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TransferProcessEventTest {

    private final TypeManager typeManager = new TypeManager();

    @ParameterizedTest
    @ArgumentsSource(EventInstances.class)
    void serdes(EventEnvelope<?> event) {
        var eventEnvelopeClass = event.getClass();
        var eventClass = event.getPayload().getClass();

        typeManager.registerTypes(new NamedType(eventEnvelopeClass, eventEnvelopeClass.getSimpleName()));
        typeManager.registerTypes(new NamedType(eventClass, eventClass.getSimpleName()));

        var json = typeManager.writeValueAsString(event);
        var deserialized = typeManager.readValue(json, EventEnvelope.class);

        assertThat(deserialized)
                .isInstanceOf(eventEnvelopeClass)
                .usingRecursiveComparison().isEqualTo(event);
    }

    private static class EventInstances implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            var callbacks = List.of(CallbackAddress.Builder.newInstance().uri("http://local").events(Set.of("test")).build());
            var eventBuilders = Stream.of(
                    TransferProcessCompleted.Builder.newInstance().transferProcessId("id").callbackAddresses(callbacks).build(),
                    TransferProcessDeprovisioned.Builder.newInstance().transferProcessId("id").callbackAddresses(callbacks).build(),
                    TransferProcessTerminated.Builder.newInstance().transferProcessId("id").callbackAddresses(callbacks).reason("any reason").build(),
                    TransferProcessFailed.Builder.newInstance().transferProcessId("id").callbackAddresses(callbacks).build(),
                    TransferProcessInitiated.Builder.newInstance().transferProcessId("id").callbackAddresses(callbacks).build(),
                    TransferProcessProvisioned.Builder.newInstance().transferProcessId("id").callbackAddresses(callbacks).build(),
                    TransferProcessRequested.Builder.newInstance().transferProcessId("id").callbackAddresses(callbacks).build()
            );

            return eventBuilders
                    .map(it -> EventEnvelope.Builder.newInstance()
                            .at(Clock.systemUTC().millis())
                            .id(UUID.randomUUID().toString()).payload(it)
                            .build())
                    .map(Arguments::of);
        }
    }

}
