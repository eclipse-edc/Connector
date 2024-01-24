/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.core.security;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.KeyParserRegistry;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LocalPublicKeyServiceImplTest {

    private LocalPublicKeyServiceImpl localPublicKeyService;
    private final Vault vault = mock();
    private final KeyParserRegistry keyParserRegistry = mock();

    @BeforeEach
    void setup() {
        localPublicKeyService = new LocalPublicKeyServiceImpl(vault, keyParserRegistry);
    }

    @Test
    void resolve_withCache() {
        when(keyParserRegistry.parse("value")).thenReturn(Result.success(mock(PublicKey.class)));
        localPublicKeyService.addRawKey("id", "value");
        assertThat(localPublicKeyService.resolveKey("id")).isSucceeded();
        verifyNoInteractions(vault);
    }

    @Test
    void resolve_withVault() {
        when(vault.resolveSecret("id")).thenReturn("value");
        when(keyParserRegistry.parse("value")).thenReturn(Result.success(mock(PublicKey.class)));
        assertThat(localPublicKeyService.resolveKey("id")).isSucceeded();

        verify(vault).resolveSecret("id");
    }

    @Test
    void resolve_notFound() {
        assertThat(localPublicKeyService.resolveKey("id")).isFailed();
        verify(vault).resolveSecret("id");
    }

    @Test
    void resolve_wrongKeyType() {
        when(vault.resolveSecret("id")).thenReturn("value");
        when(keyParserRegistry.parse("value")).thenReturn(Result.success(mock(PrivateKey.class)));

        assertThat(localPublicKeyService.resolveKey("id")).isFailed();
        verify(vault).resolveSecret("id");
    }

    @Test
    void resolve_wrongKeyFormat() {
        when(vault.resolveSecret("id")).thenReturn("value");
        when(keyParserRegistry.parse("value")).thenReturn(Result.failure("failure"));

        assertThat(localPublicKeyService.resolveKey("id")).isFailed();
        verify(vault).resolveSecret("id");
    }
}
