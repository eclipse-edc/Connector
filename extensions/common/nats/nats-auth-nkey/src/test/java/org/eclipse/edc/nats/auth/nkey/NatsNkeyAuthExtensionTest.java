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

import io.nats.client.NKey;
import io.nats.client.Options;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class NatsNkeyAuthExtensionTest {

    @Test
    void initialize_noSeedConfigured_doesNotRegisterOptions(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of()));

        var ext = factory.constructInstance(NatsNkeyAuthExtension.class);
        assertThatNoException().isThrownBy(() -> ext.initialize(context));

        verify(context, never()).registerService(eq(Options.class), any());
    }

    @Test
    void initialize_validSeed_registersSigningOptions(ServiceExtensionContext context, ObjectFactory factory, @TempDir Path tempDir) throws Exception {
        var nkey = NKey.createUser(new SecureRandom());
        var seedFile = tempDir.resolve("nats.nk");
        // trailing newline: seed files delivered by init containers commonly end with one
        Files.writeString(seedFile, new String(nkey.getSeed()) + "\n");

        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                NatsNkeyAuthExtension.SEED_PATH_KEY, seedFile.toString()
        )));

        var ext = factory.constructInstance(NatsNkeyAuthExtension.class);
        ext.initialize(context);

        var captor = ArgumentCaptor.forClass(Options.class);
        verify(context).registerService(eq(Options.class), captor.capture());

        var authHandler = captor.getValue().getAuthHandler();
        assertThat(authHandler).isNotNull();
        assertThat(authHandler.getID()).containsExactly(nkey.getPublicKey());
        assertThat(authHandler.getJWT()).isNull();

        // the handler must answer the server challenge: sign a nonce, verifiable with the public key
        var nonce = "nonce-challenge".getBytes(StandardCharsets.UTF_8);
        var signature = authHandler.sign(nonce);
        assertThat(NKey.fromPublicKey(nkey.getPublicKey()).verify(nonce, signature)).isTrue();
    }

    @Test
    void initialize_missingFile_throws(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                NatsNkeyAuthExtension.SEED_PATH_KEY, "/does/not/exist/nats.nk"
        )));

        var ext = factory.constructInstance(NatsNkeyAuthExtension.class);
        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() -> ext.initialize(context))
                .withCauseInstanceOf(IOException.class);
    }

    @Test
    void initialize_emptyFile_throws(ServiceExtensionContext context, ObjectFactory factory, @TempDir Path tempDir) throws Exception {
        var seedFile = tempDir.resolve("nats.nk");
        Files.writeString(seedFile, "  \n");

        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                NatsNkeyAuthExtension.SEED_PATH_KEY, seedFile.toString()
        )));

        var ext = factory.constructInstance(NatsNkeyAuthExtension.class);
        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() -> ext.initialize(context))
                .withMessageContaining("empty");
    }

    @Test
    void initialize_malformedSeed_throws(ServiceExtensionContext context, ObjectFactory factory, @TempDir Path tempDir) throws Exception {
        var seedFile = tempDir.resolve("nats.nk");
        Files.writeString(seedFile, "not-a-valid-nkey-seed");

        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                NatsNkeyAuthExtension.SEED_PATH_KEY, seedFile.toString()
        )));

        var ext = factory.constructInstance(NatsNkeyAuthExtension.class);
        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() -> ext.initialize(context))
                .withMessageContaining("valid NKey seed");
    }
}
