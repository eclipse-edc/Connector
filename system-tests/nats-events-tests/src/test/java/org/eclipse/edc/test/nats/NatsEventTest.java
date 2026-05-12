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

package org.eclipse.edc.test.nats;

import io.cloudevents.CloudEvent;
import io.cloudevents.jackson.JsonFormat;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.Nats;
import io.nats.client.PushSubscribeOptions;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.Matchers.equalTo;

public class NatsEventTest {
    public static final String DEFAULT_PORT = "8080";
    public static final String DEFAULT_PATH = "/api";
    public static final String MANAGEMENT_PORT = "8081";
    public static final String MANAGEMENT_PATH = "/api/management";
    public static final String STREAM_NAME = "test-stream";

    @Container
    static GenericContainer<?> nats = new GenericContainer<>("nats:latest")
            .withCommand("-js")
            .withExposedPorts(4222);


    @RegisterExtension
    protected RuntimeExtension runtime = new RuntimePerMethodExtension(
            new EmbeddedRuntime("control-plane-with-nats-events", ":dist:bom:controlplane-dcp-bom", ":extensions:common:events:events-nats")
                    .configurationProvider(() -> ConfigFactory.fromMap(new HashMap<>() {{
                                put("edc.iam.sts.oauth.token.url", "https://sts.com/token");
                                put("edc.iam.sts.oauth.client.id", "test-client");
                                put("edc.iam.sts.oauth.client.secret.alias", "test-alias");
                                put("web.http.port", DEFAULT_PORT);
                                put("web.http.path", DEFAULT_PATH);
                                put("web.http.control.port", String.valueOf(getFreePort()));
                                put("web.http.control.path", "/api/control");
                                put("web.http.management.port", MANAGEMENT_PORT);
                                put("web.http.management.path", MANAGEMENT_PATH);
                                put("edc.iam.sts.privatekey.alias", "privatekey");
                                put("edc.iam.sts.publickey.id", "publickey");
                                put("edc.participant.did", "did:web:someone");

                                // nats-specific config
                                put("edc.events.nats.url", "nats://localhost:" + nats.getMappedPort(4222));
                                put("edc.events.nats.stream", STREAM_NAME);
                                put("edc.events.nats.stream.create", "true");
                                put("edc.events.nats.stream.create.force", "true");
                            }})
                    )
    );

    @BeforeAll
    static void setup() {
        nats.start();
        nats.waitingFor(Wait.forListeningPort());
    }

    @AfterAll
    static void teardown() {
        nats.stop();
    }

    @AfterEach
    void cleanup() throws IOException, InterruptedException, JetStreamApiException {
        try (var nc = Nats.connect("nats://localhost:" + nats.getMappedPort(4222))) {
            var jsm = nc.jetStreamManagement();
            for (String stream : jsm.getStreamNames()) {
                jsm.deleteStream(stream);
            }
        }
    }

    @Test
    void publishEvent_expectPublished() throws IOException, JetStreamApiException, InterruptedException {
        await().untilAsserted(() -> given()
                .baseUri("http://localhost:" + DEFAULT_PORT + DEFAULT_PATH + "/check/startup")
                .get()
                .then()
                .statusCode(200)
                .log().ifValidationFails()
                .body("isSystemHealthy", equalTo(true)));

        //subscribe to the "events.>" subject
        JetStream js;
        try (var nc = Nats.connect("nats://localhost:" + nats.getMappedPort(4222))) {
            js = nc.jetStream();
            var pushOptions = PushSubscribeOptions.builder()
                    .stream(STREAM_NAME)
                    .build();
            var subscription = js.subscribe("events.>", pushOptions);


            // create an asset, expect an event
            var asset = """
                    {
                       "@context": [
                         "https://w3id.org/edc/connector/management/v2"
                       ],
                       "@id": "asset-1",
                       "@type": "Asset",
                       "properties": {
                         "description": "This asset requires Membership to view and Manufacturer (part_types=non_critical) to negotiate."
                       },
                       "dataplaneMetadata": {
                          "@type": "DataplaneMetadata",
                          "properties": {}
                       }
                    }
                    """;
            given()
                    .baseUri("http://localhost:%s%s".formatted(MANAGEMENT_PORT, MANAGEMENT_PATH))
                    .body(asset)
                    .contentType("application/json")
                    .post("/v4/assets")
                    .then()
                    .log().ifError()
                    .statusCode(200);

            // verify that an event was received
            var message = subscription.nextMessage(Duration.ofMillis(5000));
            assertThat(message).isNotNull();
            assertThat(message.getSubject()).isEqualTo("events.asset.created");
            message.ack();

            // verify the properties of the event
            var cloudEvent = new JsonFormat().deserialize(message.getData());
            assertThat(cloudEvent).isNotNull();
            assertThat(cloudEvent.getType()).isEqualTo("org.eclipse.edc.connector.controlplane.asset.spi.event.AssetCreated");
            assertThat(cloudEvent.getSource().toString()).isEqualTo("localhost");
            var json = new String(cloudEvent.getData().toBytes(), StandardCharsets.UTF_8);
            assertThat(json).matches(".*\"assetId\".*:.*\"asset-1\".*");
            assertThat(json).matches(".*\"participantContextId\".*:.*\"anonymous\".*");
        }

    }

}
