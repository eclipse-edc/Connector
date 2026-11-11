/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.crawler.spi.event;

import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import tools.jackson.databind.jsontype.NamedType;

import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TargetNodeCrawledTest {

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

    @Test
    void build_shouldFail_whenNodeIdIsMissing() {
        assertThatThrownBy(() -> TargetNodeCrawled.Builder.newInstance().success(true).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("nodeId");
    }

    private static class EventInstances implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            var events = Stream.of(
                    TargetNodeCrawled.Builder.newInstance().nodeId("node-id").success(true).build(),
                    TargetNodeCrawled.Builder.newInstance().nodeId("node-id").success(false).errorMessage("cannot connect").build()
            );

            return events
                    .map(it -> EventEnvelope.Builder.newInstance()
                            .at(Clock.systemUTC().millis())
                            .id(UUID.randomUUID().toString()).payload(it)
                            .build())
                    .map(Arguments::of);
        }
    }

}
