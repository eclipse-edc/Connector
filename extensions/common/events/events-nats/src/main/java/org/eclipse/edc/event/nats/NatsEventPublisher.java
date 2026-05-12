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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.core.format.ContentType;
import io.cloudevents.core.v1.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.PublishOptions;
import io.nats.client.impl.Headers;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.eclipse.edc.event.nats.Constants.EVENTS_SUBJECT;

public class NatsEventPublisher implements EventSubscriber {
    private final Monitor monitor;
    private final JetStream jetStream;
    private final ObjectMapper objectMapper;
    private final String hostname;
    private final JsonFormat jsonFormat = new JsonFormat();

    public NatsEventPublisher(Monitor monitor, JetStream jetStream, ObjectMapper objectMapper, String hostname) {
        this.monitor = monitor;
        this.jetStream = jetStream;
        this.objectMapper = objectMapper;
        this.hostname = hostname;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var eventName = event.getPayload().name();
        var subject = "%s.%s".formatted(EVENTS_SUBJECT, eventName);

        var opts = PublishOptions.builder().build();
        try {
            jetStream.publish(subject, getHeaders(), serialize(event), opts);
        } catch (IOException | JetStreamApiException e) {
            monitor.severe("Error publishing event", e);
        }
    }

    private <E extends Event> byte[] serialize(EventEnvelope<E> envelope) throws JsonProcessingException {
        var payload = envelope.getPayload();
        var json = objectMapper.writeValueAsBytes(payload);

        var cloudEvent = new CloudEventBuilder()
                .withId(envelope.getId())
                .withSource(URI.create(hostname))
                .withTime(OffsetDateTime.ofInstant(Instant.ofEpochMilli(envelope.getAt()), ZoneOffset.UTC))
                .withType(payload.getClass().getName())
                .withDataContentType(ContentType.JSON.toString())
                .withData(json)
                .build();

        return jsonFormat.serialize(cloudEvent);
    }

    private Headers getHeaders() {
        var h = new Headers();
        h.add("Content-Type", "application/cloudevents+json");
        return h;
    }
}
