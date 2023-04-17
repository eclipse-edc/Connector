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

package org.eclipse.edc.event.cloud.http;

import io.cloudevents.core.v1.CloudEventBuilder;
import io.cloudevents.http.HttpMessageFactory;
import io.cloudevents.http.impl.HttpMessageWriter;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;

class CloudEventsPublisher implements EventSubscriber {
    private static final String APPLICATION_JSON = "application/json";

    private final String endpoint;
    private final Monitor monitor;
    private final TypeManager typeManager;
    private final EdcHttpClient httpClient;
    private final Clock clock;
    private final Hostname hostname;

    CloudEventsPublisher(String endpoint, Monitor monitor, TypeManager typeManager, EdcHttpClient httpClient, Clock clock, Hostname hostname) {
        this.endpoint = endpoint;
        this.monitor = monitor;
        this.typeManager = typeManager;
        this.httpClient = httpClient;
        this.clock = clock;
        this.hostname = hostname;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var json = typeManager.writeValueAsBytes(event.getPayload());
        var instant = Instant.ofEpochMilli(event.getAt());
        var localDateTime = LocalDateTime.ofInstant(instant, clock.getZone());
        var cloudEvent = new CloudEventBuilder()
                .withId(event.getId())
                .withSource(URI.create(hostname.get()))
                .withType(event.getPayload().getClass().getName())
                .withTime(localDateTime.atOffset(UTC))
                .withDataContentType(APPLICATION_JSON)
                .withData(json)
                .build();

        createWriter().writeBinary(cloudEvent);
    }

    @NotNull
    private HttpMessageWriter createWriter() {
        var requestBuilder = new Request.Builder();
        return HttpMessageFactory.createWriter(requestBuilder::addHeader, body -> {
            var request = requestBuilder
                    .url(endpoint)
                    .post(RequestBody.create(body, MediaType.get(APPLICATION_JSON)))
                    .build();
            try (var response = httpClient.execute(request)) {
                if (!response.isSuccessful()) {
                    monitor.severe(format("Error sending cloud event to endpoint %s, response status: %d", endpoint, response.code()));
                }
            } catch (IOException e) {
                monitor.severe(format("Error sending event to endpoint %s", endpoint), e);
            }
        });
    }

}
