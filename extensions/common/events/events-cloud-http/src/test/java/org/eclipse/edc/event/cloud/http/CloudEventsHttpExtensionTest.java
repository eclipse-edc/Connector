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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;

@ExtendWith(RuntimePerMethodExtension.class)
public class CloudEventsHttpExtensionTest {

    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    void setUp(RuntimeExtension extension) {
        extension.setConfiguration(Map.of(
                CloudEventsHttpExtension.EDC_EVENTS_CLOUDEVENTS_ENDPOINT, "http://localhost:" + server.getPort()
        ));
    }

    @SuppressWarnings("unchecked")
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

            var expectedRequest = postRequestedFor(anyUrl())
                    .withRequestBody(equalToJson(typeManager.writeValueAsString(event)))
                    .withHeader("ce-id", equalTo("event-id"))
                    .withHeader("ce-source", equalTo("localhost"))
                    .withHeader("ce-specversion", equalTo("1.0"))
                    .withHeader("ce-type", equalTo("org.eclipse.edc.event.cloud.http.TestEvent"))
                    .withHeader("ce-time", equalTo("2022-06-22T13:17:33.723Z"));
            server.verify(expectedRequest);
        });
    }

}
