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

package org.eclipse.edc.nats.auth.nkey;

import io.nats.client.Connection;
import io.nats.client.NKey;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the full challenge-response flow against a real NATS server that only accepts the generated NKey user:
 * the extension-contributed {@link Options} must authenticate, an anonymous connection must be refused.
 */
@Testcontainers
@EndToEndTest
@ExtendWith(DependencyInjectionExtension.class)
class NatsNkeyAuthIntegrationTest {

    private static NKey nkey;
    private static GenericContainer<?> nats;

    @BeforeAll
    static void setup() throws Exception {
        nkey = NKey.createUser(new SecureRandom());
        var natsConf = """
                port: 4222
                authorization {
                  users [
                    { nkey: "%s" }
                  ]
                }
                """.formatted(new String(nkey.getPublicKey()));
        nats = new GenericContainer<>("nats:latest")
                .withCopyToContainer(Transferable.of(natsConf), "/etc/nats/nats.conf")
                .withCommand("-c", "/etc/nats/nats.conf")
                .withExposedPorts(4222)
                .waitingFor(Wait.forListeningPort());
        nats.start();
    }

    @AfterAll
    static void teardown() {
        if (nats != null) {
            nats.stop();
        }
    }

    @Test
    void connect_withExtensionOptions_succeeds(ServiceExtensionContext context, ObjectFactory factory, @TempDir Path tempDir) throws Exception {
        var seedFile = tempDir.resolve("nats.nk");
        Files.writeString(seedFile, new String(nkey.getSeed()));
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                NatsNkeyAuthExtension.SEED_PATH_KEY, seedFile.toString()
        )));

        var ext = factory.constructInstance(NatsNkeyAuthExtension.class);
        ext.initialize(context);

        var captor = ArgumentCaptor.forClass(Options.class);
        verify(context).registerService(eq(Options.class), captor.capture());

        // consume the contributed options exactly like the NATS extensions do: copy, then set the server
        var options = new Options.Builder(captor.getValue())
                .server(natsUrl())
                .connectionTimeout(Duration.ofSeconds(5))
                .build();
        try (var connection = Nats.connect(options)) {
            assertThat(connection.getStatus()).isEqualTo(Connection.Status.CONNECTED);
        }
    }

    @Test
    void connect_anonymous_isRefused() {
        assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> Nats.connect(natsUrl()))
                .withMessageContaining("Authorization Violation");
    }

    private static String natsUrl() {
        return "nats://localhost:" + nats.getMappedPort(4222);
    }
}
