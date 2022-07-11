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
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.interfaces.DHPrivateKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurablePrivateKeyResolverTest {

    private static final String TEST_SECRET_ALIAS = "test-secret";

    private Map<String, String> cache;
    private ConfigurablePrivateKeyResolver resolver;

    @BeforeEach
    void setUp() {
        cache = new HashMap<>();
        resolver = new MockPrivateKeyResolver(cache);
        resolver.addParser(new DummyParser());
    }

    @Test
    void resolvePrivateKey() {
        cache.put(TEST_SECRET_ALIAS, PrivateTestKeys.ENCODED_PRIVATE_KEY_HEADER);

        var result = resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class);

        assertThat(result).isNotNull();
    }

    @Test
    void resolvePrivateKey_encodedKeyNotFound() {
        var result = resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class);

        assertThat(result).isNull();
    }

    @Test
    void resolvePrivateKey_encodedKeyNotInCorrectFormat() {
        cache.put(TEST_SECRET_ALIAS, PrivateTestKeys.ENCODED_PRIVATE_KEY_NOPEM);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class));
    }

    @Test
    void resolvePrivateKey_noParserFound() {
        cache.put(TEST_SECRET_ALIAS, PrivateTestKeys.ENCODED_PRIVATE_KEY_HEADER);

        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() -> resolver.resolvePrivateKey(TEST_SECRET_ALIAS, DHPrivateKey.class))
                .withMessageContaining("Cannot find KeyParser for type");
    }

    @Test
    void addParser() {
        cache.put(TEST_SECRET_ALIAS, PrivateTestKeys.ENCODED_PRIVATE_KEY_HEADER);

        resolver = new MockPrivateKeyResolver(cache);
        // no parsers present
        assertThatThrownBy(() -> resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isInstanceOf(EdcException.class);
        resolver.addParser(new DummyParser());

        // same resolve call should work now
        assertThat(resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isNotNull();
    }

    @Test
    void testAddParser() {
        cache.put(TEST_SECRET_ALIAS, PrivateTestKeys.ENCODED_PRIVATE_KEY_HEADER);

        resolver = new MockPrivateKeyResolver(cache);
        // no parsers present
        assertThatThrownBy(() -> resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isInstanceOf(EdcException.class);
        resolver.addParser(RSAPrivateKey.class, s -> new DummyParser().parse(s));

        // same resolve call should work now
        assertThat(resolver.resolvePrivateKey(TEST_SECRET_ALIAS, RSAPrivateKey.class)).isNotNull();
    }

    private static class MockPrivateKeyResolver extends ConfigurablePrivateKeyResolver {

        private final Map<String, String> cache;

        private MockPrivateKeyResolver(Map<String, String> cache) {
            this.cache = cache;
        }

        @Override
        protected @Nullable String getEncodedKey(String id) {
            return cache.get(id);
        }
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

