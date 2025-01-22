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

import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;

import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@ExtendWith(RuntimePerMethodExtension.class)
public class CloudEventsHttpExtensionTest {

    private final int port = getFreePort();
    private final ClientAndServer server = startClientAndServer(port);

    @BeforeEach
    void setUp(RuntimeExtension extension) {
        extension.setConfiguration(Map.of(
                CloudEventsHttpExtension.EDC_EVENTS_CLOUDEVENTS_ENDPOINT, "http://localhost:" + port
        ));
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void shouldSendEventAccordingToCloudEventSpec(EventRouter eventRouter, TypeManager typeManager) {
        var event = TestEvent.Builder.newInstance().data("useful information").build();

        var envelope = EventEnvelope.Builder.newInstance()
                .id("event-id")
                .payload(event)
                .at(1655903853723L)
                .build();

        eventRouter.publish(envelope);

        await().untilAsserted(() -> {
            var expectedRequest = HttpRequest.request()
                    .withBody(new JsonBody(typeManager.writeValueAsString(event)))
                    .withHeader("ce-id", "event-id")
                    .withHeader("ce-source", "localhost")
                    .withHeader("ce-specversion", "1.0")
                    .withHeader("ce-type", "org.eclipse.edc.event.cloud.http.TestEvent")
                    .withHeader("ce-time", "2022-06-22T13:17:33.723Z");
            server.verify(expectedRequest);
        });
    }

}
