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

package org.eclipse.edc.event.nats;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.PublishOptions;
import io.nats.client.api.Error;
import io.nats.client.impl.Headers;
import io.nats.client.support.Status;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EndToEndTest
class NatsEventPublisherTest {
    private final JetStream jetstream = mock();
    private final Monitor monitor = mock();
    private final NatsEventPublisher natsEventPublisher = new NatsEventPublisher(monitor, jetstream, new ObjectMapper(), "localhost/test");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void onEvent_shouldPublish() throws JetStreamApiException, IOException {
        natsEventPublisher.on(payload(new DummyEvent("foobar")));

        verify(monitor, never()).severe(anyString(), isA(Throwable[].class));
        verify(jetstream).publish(eq("events.dummy.foobar"), any(Headers.class), notNull(), any(PublishOptions.class));
    }


    @Test
    void onEvent_publishFails() throws JetStreamApiException, IOException {
        when(jetstream.publish(anyString(), any(Headers.class), any(byte[].class), any(PublishOptions.class)))
                .thenThrow(new JetStreamApiException(Error.JsBadRequestErr));

        natsEventPublisher.on(payload(new DummyEvent("foobar")));

        verify(monitor).severe(anyString(), isA(JetStreamApiException.class));
        verify(jetstream).publish(eq("events.dummy.foobar"), any(Headers.class), notNull(), any(PublishOptions.class));
    }

    @SuppressWarnings("unchecked")
    private EventEnvelope<Event> payload(DummyEvent foobar) {
        return EventEnvelope.Builder.newInstance()
                .at(Instant.now().toEpochMilli())
                .id(UUID.randomUUID().toString())
                .payload(foobar)
                .build();
    }

    public static class DummyEvent extends Event {

        private String data;

        DummyEvent(String data) {
            this.data = data;
        }

        @Override
        public String name() {
            return "dummy.foobar";
        }

        public String getData() {
            return data;
        }
    }
}