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
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.PublishOptions;
import io.nats.client.impl.Headers;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;

import static org.eclipse.edc.event.nats.Constants.EVENTS_SUBJECT;

public class NatsEventPublisher implements EventSubscriber {
    private final Monitor monitor;
    private final JetStream jetStream;
    private final ObjectMapper objectMapper;

    public NatsEventPublisher(Monitor monitor, JetStream jetStream, ObjectMapper objectMapper) {
        this.monitor = monitor;
        this.jetStream = jetStream;
        this.objectMapper = objectMapper;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var eventName = event.getPayload().name();
        var subject = "%s.%s".formatted(EVENTS_SUBJECT, eventName);

        var opts = PublishOptions.builder().build();
        try {
            jetStream.publish(subject, getHeaders(), serialize(event.getPayload()), opts);
        } catch (IOException | JetStreamApiException e) {
            monitor.severe("Error publishing event", e);
        }
    }

    private byte[] serialize(Object payload) throws JsonProcessingException {
        // todo: serialize as CloudEvents
        return objectMapper.writeValueAsBytes(payload);
    }

    private Headers getHeaders() {
        return null;
    }
}
