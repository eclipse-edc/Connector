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

package org.eclipse.edcnats;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStreamApiException;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.StreamConfiguration;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.event.nats.NatsEventPublisher;
import org.eclipse.edc.event.nats.NatsPublishingExtension;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Testcontainers
@EndToEndTest
@ExtendWith(DependencyInjectionExtension.class)
class NatsPublishingExtensionTest {
    private final EventRouter eventRouter = mock();

    @Container
    static GenericContainer<?> nats = new GenericContainer<>("nats:latest")
            .withCommand("-js")
            .withExposedPorts(4222);

    @BeforeAll
    static void setup() {
        nats.start();
        nats.waitingFor(Wait.forListeningPort());
    }

    @AfterAll
    static void teardown() {
        nats.stop();
    }

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(EventRouter.class, eventRouter);
        context.registerService(ObjectMapper.class, new ObjectMapper());
        context.registerService(Options.class, null);
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
    void initialize(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.events.nats.url", "nats://localhost:" + nats.getMappedPort(4222),
                "edc.events.nats.stream", "test-stream",
                "edc.events.nats.stream.create", "true"
        )));
        var ext = factory.constructInstance(NatsPublishingExtension.class);
        assertThatNoException().isThrownBy(() -> ext.initialize(context));
    }

    @Test
    void initialize_natsConnectionFails(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.events.nats.url", "nats://localhost:4222",
                "edc.events.nats.stream", "test-stream",
                "edc.events.nats.stream.create.force", "true"
        )));
        var ext = factory.constructInstance(NatsPublishingExtension.class);
        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> ext.initialize(context));
    }

    @Test
    void initialize_noCreateStream(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.events.nats.url", "nats://localhost:" + nats.getMappedPort(4222),
                "edc.events.nats.stream", "test-stream",
                "edc.events.nats.stream.create", "false"
        )));
        var ext = factory.constructInstance(NatsPublishingExtension.class);
        assertThatNoException().isThrownBy(() -> ext.initialize(context));
    }

    @Test
    void initialize_streamExists_noForce(ServiceExtensionContext context, ObjectFactory factory) throws Exception {
        var streamName = UUID.randomUUID().toString();
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.events.nats.url", "nats://localhost:" + nats.getMappedPort(4222),
                "edc.events.nats.stream", streamName,
                "edc.events.nats.stream.create", "true",
                "edc.events.nats.stream.create.force", "false"
        )));

        // create stream - will cause collision
        try (var conn = Nats.connect("localhost:" + nats.getMappedPort(4222))) {
            var jsm = conn.jetStreamManagement();
            jsm.addStream(StreamConfiguration.builder().name(streamName).build());

            var ext = factory.constructInstance(NatsPublishingExtension.class);
            assertThatExceptionOfType(EdcException.class).isThrownBy(() -> ext.initialize(context));
        }
    }

    @Test
    void initialize_streamExists_withForce(ServiceExtensionContext context, ObjectFactory factory) throws Exception {
        var streamName = UUID.randomUUID().toString();
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.events.nats.url", "nats://localhost:" + nats.getMappedPort(4222),
                "edc.events.nats.stream", streamName,
                "edc.events.nats.stream.create", "true",
                "edc.events.nats.stream.create.force", "true"
        )));

        // create stream - will cause collision
        try (var conn = Nats.connect("localhost:" + nats.getMappedPort(4222))) {
            var jsm = conn.jetStreamManagement();
            jsm.addStream(StreamConfiguration.builder().name(streamName).build());
        }

        var ext = factory.constructInstance(NatsPublishingExtension.class);
        assertThatNoException().isThrownBy(() -> ext.initialize(context));
    }


    @Test
    void prepare(ServiceExtensionContext context, ObjectFactory factory) throws Exception {
        var streamName = UUID.randomUUID().toString();
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.events.nats.url", "nats://localhost:" + nats.getMappedPort(4222),
                "edc.events.nats.stream", streamName,
                "edc.events.nats.stream.create", "true",
                "edc.events.nats.stream.create.force", "true"
        )));

        // create stream - will cause collision
        try (var conn = Nats.connect("localhost:" + nats.getMappedPort(4222))) {
            var jsm = conn.jetStreamManagement();
            jsm.addStream(StreamConfiguration.builder().name(streamName).build());
        }

        var ext = factory.constructInstance(NatsPublishingExtension.class);
        assertThatNoException().isThrownBy(() -> ext.initialize(context));
        assertThatNoException().isThrownBy(ext::prepare);

        verify(eventRouter).register(eq(Event.class), isA(NatsEventPublisher.class));
    }
}