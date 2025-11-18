/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.keys;

import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.ParticipantVault;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class VaultPrivateKeyResolverTest {

    private static final String TEST_SECRET_ALIAS = "test-secret";
    private final Vault vault = mock();
    private final ParticipantVault participantVault = mock();
    private VaultPrivateKeyResolver resolver;
    private Config config;
    private KeyParserRegistry registry;
    private final String participantContextId = "test-participant";

    @BeforeEach
    void setUp() {
        config = mock();
        registry = mock();
        when(registry.parse(any())).thenReturn(Result.failure("foo"));
        resolver = new VaultPrivateKeyResolver(registry, participantVault, mock(), config, vault);
    }

    @Test
    void resolvePrivateKey() {
        when(participantVault.resolveSecret(eq(participantContextId), eq(TEST_SECRET_ALIAS))).thenReturn(PrivateTestKeys.ENCODED_PRIVATE_KEY_HEADER);
        when(registry.parse(any())).thenReturn(Result.success(createKey()));
        var result = resolver.resolvePrivateKey(participantContextId, TEST_SECRET_ALIAS);

        assertThat(result).isNotNull();
        verify(participantVault, atLeastOnce()).resolveSecret(eq(participantContextId), eq(TEST_SECRET_ALIAS));
        verifyNoMoreInteractions(participantVault, vault);
    }

    @Test
    void resolvePrivateKey_secretNotFound() {
        var result = resolver.resolvePrivateKey(participantContextId, TEST_SECRET_ALIAS);
        assertThat(result)
                .isFailed()
                .detail().startsWith("Private key with ID 'test-secret' not found in Config");
    }

    @Test
    void resolvePrivateKey_notFoundInVault_fallbackToConfig() {
        when(participantVault.resolveSecret(eq(participantContextId), eq(TEST_SECRET_ALIAS))).thenReturn(null);
        when(config.getString(eq(TEST_SECRET_ALIAS), any())).thenReturn("{}");
        when(registry.parse(eq("{}"))).thenReturn(Result.success(createKey()));

        var result = resolver.resolvePrivateKey(participantContextId, TEST_SECRET_ALIAS);
        assertThat(result).isNotNull();
        assertThat(result).isSucceeded().isNotNull().isInstanceOf(RSAPrivateKey.class);

        verify(participantVault, atLeastOnce()).resolveSecret(eq(participantContextId), eq(TEST_SECRET_ALIAS));
        verifyNoMoreInteractions(participantVault, vault);
    }


    @Test
    void resolvePrivateKey_noParserCanHandle() {
        when(participantVault.resolveSecret(eq(participantContextId), eq(TEST_SECRET_ALIAS))).thenReturn(PrivateTestKeys.ENCODED_PRIVATE_KEY_NOPEM);

        var result = resolver.resolvePrivateKey(participantContextId, TEST_SECRET_ALIAS);
        assertThat(result).isFailed()
                .detail()
                .isEqualTo("foo");
        verify(participantVault, atLeastOnce()).resolveSecret(eq(participantContextId), eq(TEST_SECRET_ALIAS));
        verifyNoMoreInteractions(participantVault, vault);
    }

    @Test
    void resolvePrivateKey_noParserFound() {
        when(participantVault.resolveSecret(eq(participantContextId), eq(TEST_SECRET_ALIAS))).thenReturn(PrivateTestKeys.ENCODED_PRIVATE_KEY_NOPEM);

        var result = resolver.resolvePrivateKey(participantContextId, TEST_SECRET_ALIAS);
        assertThat(result).isFailed()
                .detail()
                .isEqualTo("foo");
        verify(participantVault, atLeastOnce()).resolveSecret(eq(participantContextId), eq(TEST_SECRET_ALIAS));
    }

    private PrivateKey createKey() {
        try {
            var pk = KeyPairGenerator.getInstance("RSA");
            pk.initialize(1024);
            return pk.generateKeyPair().getPrivate();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}

