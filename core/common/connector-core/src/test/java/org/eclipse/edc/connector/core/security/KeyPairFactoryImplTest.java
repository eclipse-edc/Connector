/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.core.security;

import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeyPairFactoryImplTest {

    private final PrivateKeyResolver privateKeyResolver = mock(PrivateKeyResolver.class);
    private final Vault vault = mock(Vault.class);

    private final KeyPairFactoryImpl factory = new KeyPairFactoryImpl(privateKeyResolver, vault);

    private static String loadPemFile(String file) throws IOException {
        return new String(Objects.requireNonNull(KeyPairFactoryImpl.class.getClassLoader().getResourceAsStream(file))
                .readAllBytes());
    }

    @ParameterizedTest(name = "{index} {1}")
    @CsvSource({ "rsa_2048.pem, RSA", "ec_p384.pem, EC" })
    void fromConfig_success(String keyFileName, String expectedAlgo) throws IOException {
        var privateKeyAlias = UUID.randomUUID().toString();
        var publicKeyAlias = UUID.randomUUID().toString();
        var privateKey = mock(PrivateKey.class);
        var publicKeyPem = loadPemFile(keyFileName);

        when(privateKeyResolver.resolvePrivateKey(privateKeyAlias, PrivateKey.class)).thenReturn(privateKey);
        when(vault.resolveSecret(publicKeyAlias)).thenReturn(publicKeyPem);

        var result = factory.fromConfig(publicKeyAlias, privateKeyAlias);

        assertThat(result.succeeded()).isTrue();
        var keyPair = result.getContent();
        assertThat(keyPair.getPrivate()).isEqualTo(privateKey);
        assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo(expectedAlgo);
    }

    @Test
    void fromConfig_failedToRetrievePrivateKey() {
        var privateKeyAlias = UUID.randomUUID().toString();
        var publicKeyAlias = UUID.randomUUID().toString();

        when(privateKeyResolver.resolvePrivateKey(privateKeyAlias, PrivateKey.class)).thenReturn(null);
        when(vault.resolveSecret(publicKeyAlias)).thenReturn("pem");

        var result = factory.fromConfig(publicKeyAlias, privateKeyAlias);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void fromConfig_failedToRetrievePublicKey() {
        var privateKeyAlias = UUID.randomUUID().toString();
        var publicKeyAlias = UUID.randomUUID().toString();

        when(privateKeyResolver.resolvePrivateKey(privateKeyAlias, PrivateKey.class)).thenReturn(mock(PrivateKey.class));
        when(vault.resolveSecret(publicKeyAlias)).thenReturn(null);

        var result = factory.fromConfig(publicKeyAlias, privateKeyAlias);

        assertThat(result.failed()).isTrue();
    }
}