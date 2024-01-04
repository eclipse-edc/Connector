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

package org.eclipse.edc.jwt;

import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.RSASSASigner;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JwsSignerConverterTest {

    private final JwsSignerConverter factory = new JwsSignerConverterImpl();

    private static PrivateKey createEd25519(@Nullable Provider provider) throws NoSuchAlgorithmException {

        KeyPairGenerator kpg = provider == null ?
                KeyPairGenerator.getInstance("Ed25519") :
                KeyPairGenerator.getInstance("Ed25519", provider);
        return kpg.generateKeyPair().getPrivate();
    }

    private static PrivateKey createEc() throws NoSuchAlgorithmException {
        var gen = KeyPairGenerator.getInstance("EC");
        return gen.generateKeyPair().getPrivate();
    }

    private static PrivateKey createRsa() throws NoSuchAlgorithmException {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair().getPrivate();
    }

    @Test
    void createSignerFor_rsaKey() throws NoSuchAlgorithmException {
        var pk = createRsa();

        assertThat(factory.createSignerFor(pk)).isInstanceOf(RSASSASigner.class);
    }

    @Test
    void createSignerFor_ecKey() throws NoSuchAlgorithmException {
        var pk = createEc();

        assertThat(factory.createSignerFor(pk)).isInstanceOf(ECDSASigner.class);
    }

    @Test
    void createSignerFor_edDsaKey_sunProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(null);
        assertThat(factory.createSignerFor(kp)).isInstanceOf(Ed25519Signer.class);
    }

    @Test
    void createSignerFor_edDsaKey_bouncyCastleProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(new BouncyCastleProvider());
        assertThat(factory.createSignerFor(kp)).isInstanceOf(Ed25519Signer.class);
    }

    @ParameterizedTest
    @ArgumentsSource(PrivateKeyProvider.class)
    void getAlgorithm(PrivateKey key) {
        var signer = factory.createSignerFor(key);
        assertThat(factory.getRecommendedAlgorithm(signer)).isNotNull();
    }

    private static class PrivateKeyProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(Named.named("RSA", createRsa())),
                    Arguments.of(Named.named("EC", createEc())),
                    Arguments.of(Named.named("Ed25519 (BC)", createEd25519(new BouncyCastleProvider()))),
                    Arguments.of(Named.named("Ed25519 (Sun)", createEd25519(null)))
            );
        }
    }
}