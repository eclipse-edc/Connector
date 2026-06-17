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
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NatsEventPublisherTest {
    private final JetStream jetstream = mock();
    private final Monitor monitor = mock();
    private final NatsEventPublisher natsEventPublisher = new NatsEventPublisher(monitor, jetstream, new ObjectMapper(), "localhost/test", new Telemetry());

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

    @Test
    void onEvent_shouldInjectTraceContextIntoHeaders() throws JetStreamApiException, IOException {
        var traceparent = "00-0102030405060708090a0b0c0d0e0f10-0102030405060708-01";
        var telemetry = mock(Telemetry.class);
        when(telemetry.getCurrentTraceContext()).thenReturn(Map.of("traceparent", traceparent));
        var publisher = new NatsEventPublisher(monitor, jetstream, new ObjectMapper(), "localhost/test", telemetry);
        var headers = ArgumentCaptor.forClass(Headers.class);

        publisher.on(payload(new DummyEvent("foobar")));

        verify(jetstream).publish(eq("events.dummy.foobar"), headers.capture(), notNull(), any(PublishOptions.class));
        assertThat(headers.getValue().get("traceparent")).containsExactly(traceparent);
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
