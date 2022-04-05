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
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.resolver;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SelfPublicKeyResolverTest {

    private static final String KEY_ALIAS = "alias-test";

    @ParameterizedTest
    @CsvSource({"rsa-pubkey.pem, RSA", "ec-pubkey.pem, EC"})
    void verifyResolveOk(String keyFileName, String expectedAlgo) {
        var vault = mock(Vault.class);
        var publicKeyPem = loadPublicKeyFromResourceFile(keyFileName);
        when(vault.resolveSecret(KEY_ALIAS)).thenReturn(publicKeyPem);
        var resolver = new SelfPublicKeyResolver(vault, KEY_ALIAS);

        var key = resolver.resolveKey(UUID.randomUUID().toString());

        assertThat(key).isNotNull();
        assertThat(key.getAlgorithm()).isEqualTo(expectedAlgo);
    }

    private static String loadPublicKeyFromResourceFile(String file) {
        try (InputStream in = SelfPublicKeyResolverTest.class.getClassLoader().getResourceAsStream(file)) {
            Objects.requireNonNull(in);
            return new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new EdcException("Failed load public key from file");
        }
    }
}