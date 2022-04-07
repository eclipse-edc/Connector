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

package org.eclipse.dataspaceconnector.spi.security;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.interfaces.DHPrivateKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VaultPrivateKeyResolverTest {

    private static final String TEST_SECRET_ALIAS = "test-secret";
    private final Vault vault = mock(Vault.class);
    private VaultPrivateKeyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new VaultPrivateKeyResolver(vault);
        resolver.addParser(new DummyParser());
    }

    @Test
    void resolvePrivateKey() {
        when(vault.resolveSecret(TEST_SECRET_ALIAS)).thenReturn(PrivateTestKeys.ENCODED_PRIVATE_KEY_HEADER);

        var result = resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class);

        assertThat(result).isNotNull();
        verify(vault, atLeastOnce()).resolveSecret(TEST_SECRET_ALIAS);
    }

    @Test
    void resolvePrivateKey_secretNotFound() {
        var result = resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class);

        assertThat(result).isNull();
    }

    @Test
    void resolvePrivateKey_secretNotInCorrectFormat() {
        when(vault.resolveSecret(TEST_SECRET_ALIAS)).thenReturn(PrivateTestKeys.ENCODED_PRIVATE_KEY_NOPEM);

        assertThatThrownBy(() -> resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isInstanceOf(IllegalArgumentException.class);
        verify(vault, atLeastOnce()).resolveSecret(TEST_SECRET_ALIAS);
    }

    @Test
    void resolvePrivateKey_noParserFound() {
        when(vault.resolveSecret(TEST_SECRET_ALIAS)).thenReturn(PrivateTestKeys.ENCODED_PRIVATE_KEY_NOPEM);

        assertThatThrownBy(() -> resolver.resolvePrivateKey(TEST_SECRET_ALIAS, DHPrivateKey.class)).isInstanceOf(EdcException.class)
                .hasMessageStartingWith("Cannot find KeyParser for type");
        verify(vault, atLeastOnce()).resolveSecret(TEST_SECRET_ALIAS);
    }

    @Test
    void addParser() {
        when(vault.resolveSecret(TEST_SECRET_ALIAS)).thenReturn(PrivateTestKeys.ENCODED_PRIVATE_KEY_HEADER);

        resolver = new VaultPrivateKeyResolver(vault);
        // no parsers present
        assertThatThrownBy(() -> resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isInstanceOf(EdcException.class);
        resolver.addParser(new DummyParser());

        //same resolve call should work now
        assertThat(resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isNotNull();
        verify(vault, atLeastOnce()).resolveSecret(TEST_SECRET_ALIAS);
    }

    @Test
    void testAddParser() {
        when(vault.resolveSecret(TEST_SECRET_ALIAS)).thenReturn(PrivateTestKeys.ENCODED_PRIVATE_KEY_HEADER);

        resolver = new VaultPrivateKeyResolver(vault);
        // no parsers present
        assertThatThrownBy(() -> resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isInstanceOf(EdcException.class);
        resolver.addParser(RSAPrivateKey.class, s -> new DummyParser().parse(s));

        //same resolve call should work now
        assertThat(resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isNotNull();
        verify(vault, atLeastOnce()).resolveSecret(TEST_SECRET_ALIAS);
    }

    private static class DummyParser implements KeyParser<RSAPrivateKey> {

        private static final String PEM_HEADER = "-----BEGIN PRIVATE KEY-----";
        private static final String PEM_FOOTER = "-----END PRIVATE KEY-----";

        @Override
        public boolean canParse(Class<?> keyType) {
            return keyType.equals(RSAPrivateKey.class);
        }

        @Override
        public RSAPrivateKey parse(String entirePemFileContent) {
            entirePemFileContent = entirePemFileContent.replace(PEM_HEADER, "").replaceAll(System.lineSeparator(), "").replace(PEM_FOOTER, "");
            entirePemFileContent = entirePemFileContent.replace("\n", ""); //base64 might complain if newlines are present

            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(entirePemFileContent.getBytes())));

            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

