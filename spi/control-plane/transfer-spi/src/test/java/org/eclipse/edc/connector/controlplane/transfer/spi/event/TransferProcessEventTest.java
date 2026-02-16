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

package org.eclipse.edc.connector.controlplane.transfer.spi.event;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
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

    private final TypeManager typeManager = new JacksonTypeManager();

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

        @SuppressWarnings("unchecked")
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            var eventBuilders = Stream.of(
                    TransferProcessCompleted.Builder.newInstance(),
                    TransferProcessDeprovisioned.Builder.newInstance(),
                    TransferProcessDeprovisioningRequested.Builder.newInstance(),
                    TransferProcessStarted.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("type").build()),
                    TransferProcessTerminated.Builder.newInstance().reason("any reason"),
                    TransferProcessInitiated.Builder.newInstance(),
                    TransferProcessProvisioned.Builder.newInstance(),
                    TransferProcessPrepared.Builder.newInstance(),
                    TransferProcessPreparationRequested.Builder.newInstance(),
                    TransferProcessRequested.Builder.newInstance().transferProcessId("id")
            );

            return eventBuilders
                    .map(it -> baseProperties(it).build())
                    .map(it -> EventEnvelope.Builder.newInstance()
                            .at(Clock.systemUTC().millis())
                            .id(UUID.randomUUID().toString()).payload(it)
                            .build())
                    .map(Arguments::of);
        }

        private <T extends TransferProcessEvent, B extends TransferProcessEvent.Builder<T, B>> TransferProcessEvent.Builder<T, B> baseProperties(TransferProcessEvent.Builder<T, B> builder) {
            var callbacks = List.of(CallbackAddress.Builder.newInstance().uri("http://local").events(Set.of("test")).build());
            return builder.transferProcessId("id")
                    .assetId("assetId")
                    .type(TransferProcess.Type.CONSUMER.name())
                    .contractId("agreementId")
                    .callbackAddresses(callbacks);
        }
    }

}
