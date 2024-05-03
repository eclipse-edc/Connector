/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.contract.spi.event;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.eclipse.edc.connector.secret.spi.event.SecretCreated;
import org.eclipse.edc.connector.secret.spi.event.SecretDeleted;
import org.eclipse.edc.connector.secret.spi.event.SecretUpdated;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SecretEventTest {

    private final TypeManager typeManager = new JacksonTypeManager();

    @ParameterizedTest
    @ArgumentsSource(EventInstances.class)
    void serDes(EventEnvelope<?> event) {
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

            var eventBuilders = Stream.of(
                    SecretCreated.Builder.newInstance().secretId("id").build(),
                    SecretDeleted.Builder.newInstance().secretId("id").build(),
                    SecretUpdated.Builder.newInstance().secretId("id").build()
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
