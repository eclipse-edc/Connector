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

package org.eclipse.edc.jwt.spi;

import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JwsSignerVerifierFactoryTest {

    private final JwsSignerVerifierFactory factory = new JwsSignerVerifierFactory();

    private static KeyPair createEd25519(@Nullable Provider provider) throws NoSuchAlgorithmException {

        KeyPairGenerator kpg = provider == null ?
                KeyPairGenerator.getInstance("Ed25519") :
                KeyPairGenerator.getInstance("Ed25519", provider);
        return kpg.generateKeyPair();
    }

    private static KeyPair createEc() throws NoSuchAlgorithmException {
        var gen = KeyPairGenerator.getInstance("EC");
        return gen.generateKeyPair();
    }

    private static KeyPair createRsa() throws NoSuchAlgorithmException {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    @Test
    void createSignerFor_rsaKey() throws NoSuchAlgorithmException {
        var pk = createRsa();

        assertThat(factory.createSignerFor(pk.getPrivate())).isInstanceOf(RSASSASigner.class);
    }

    @Test
    void createSignerFor_ecKey() throws NoSuchAlgorithmException {
        var pk = createEc();

        assertThat(factory.createSignerFor(pk.getPrivate())).isInstanceOf(ECDSASigner.class);
    }

    @Test
    void createSignerFor_edDsaKey_sunProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(null);
        assertThat(factory.createSignerFor(kp.getPrivate())).isInstanceOf(Ed25519Signer.class);
    }

    @Test
    void createSignerFor_edDsaKey_bouncyCastleProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(new BouncyCastleProvider());
        assertThat(factory.createSignerFor(kp.getPrivate())).isInstanceOf(Ed25519Signer.class);
    }

    @Test
    void createVerifierFor_rsaKey() throws NoSuchAlgorithmException {
        var pk = createRsa().getPublic();
        assertThat(factory.createVerifierFor(pk)).isInstanceOf(RSASSAVerifier.class);
    }

    @Test
    void createVerifierFor_ecKey() throws NoSuchAlgorithmException {
        var pk = createEc().getPublic();
        assertThat(factory.createVerifierFor(pk)).isInstanceOf(ECDSAVerifier.class);
    }

    @Test
    void createVerifierFor_edDsaKey_sunProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(null);
        assertThat(factory.createVerifierFor(kp.getPublic())).isInstanceOf(Ed25519Verifier.class);
    }

    @Test
    void createVerifierFor_edDsaKey_bouncyCastleProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(new BouncyCastleProvider());
        assertThat(factory.createVerifierFor(kp.getPublic())).isInstanceOf(Ed25519Verifier.class);
    }

    @ParameterizedTest
    @ArgumentsSource(KeyProvider.class)
    void getJwsAlgorithm(KeyPair keypair) {
        var signer = factory.createSignerFor(keypair.getPrivate());
        assertThat(factory.getRecommendedAlgorithm(signer)).isNotNull();
    }

    private static class KeyProvider implements ArgumentsProvider {
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