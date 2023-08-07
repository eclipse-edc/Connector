/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.security.signature.jws2020;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.security.signature.jws2020.TestFunctions.readResourceAsString;

class KeyFactoryTest {

    @Test
    void create_ecKey() throws JsonProcessingException {
        var json = """
                {
                    "kty": "EC",
                    "crv": "P-384",
                    "x": "eQbMauiHc9HuiqXT894gW5XTCrOpeY8cjLXAckfRtdVBLzVHKaiXAAxBFeVrSB75",
                    "y": "YOjxhMkdH9QnNmGCGuGXJrjAtk8CQ1kTmEEi9cg2R9ge-zh8SFT1Xu6awoUjK5Bv",
                    "d": "dXghMAzYZmv46SNRuxmfDIuAlv7XIhvlkPzW3vXsopB1ihWp47tx0hqjZmYO6fJa"
                }
                """;

        assertThat(KeyFactory.create(json)).isInstanceOf(ECKey.class).extracting(JWK::isPrivate).isEqualTo(true);
        var map = new ObjectMapper().readValue(json, Map.class);
        assertThat(KeyFactory.create(map)).isInstanceOf(ECKey.class).extracting(JWK::isPrivate).isEqualTo(true);
    }

    @Test
    void create_rsa() throws JsonProcessingException {
        // the RSA key would violate the Checkstyle line length constraint
        var json = readResourceAsString("rsakey.json");

        assertThat(KeyFactory.create(json)).isInstanceOf(RSAKey.class).extracting(JWK::isPrivate).isEqualTo(true);
        var map = new ObjectMapper().readValue(json, Map.class);
        assertThat(KeyFactory.create(map)).isInstanceOf(RSAKey.class).extracting(JWK::isPrivate).isEqualTo(true);
    }

    @Test
    void create_okp() throws JsonProcessingException {
        var json = """
                {
                   "kty" : "OKP",
                   "crv" : "Ed25519",
                   "x"   : "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo",
                   "d"   : "nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A",
                   "use" : "sig",
                   "kid" : "FdFYFzERwC2uCBB46pZQi4GG85LujR8obt-KWRBICVQ"
                 }
                """;

        assertThat(KeyFactory.create(json)).isInstanceOf(OctetKeyPair.class).extracting(JWK::isPrivate).isEqualTo(true);
        var map = new ObjectMapper().readValue(json, Map.class);
        assertThat(KeyFactory.create(map)).isInstanceOf(OctetKeyPair.class).extracting(JWK::isPrivate).isEqualTo(true);
    }

    @Test
    void create_invalidJson() throws JsonProcessingException {
        // JSON misses the "crv" property
        var json = """
                {
                    "kty": "EC",
                    "x": "eQbMauiHc9HuiqXT894gW5XTCrOpeY8cjLXAckfRtdVBLzVHKaiXAAxBFeVrSB75",
                    "y": "YOjxhMkdH9QnNmGCGuGXJrjAtk8CQ1kTmEEi9cg2R9ge-zh8SFT1Xu6awoUjK5Bv"
                }
                """;

        assertThatThrownBy(() -> assertThat(KeyFactory.create(json))).isInstanceOf(RuntimeException.class).rootCause().isInstanceOf(ParseException.class);

        var map = new ObjectMapper().readValue(json, Map.class);
        assertThatThrownBy(() -> assertThat(KeyFactory.create(map))).isInstanceOf(RuntimeException.class).rootCause().isInstanceOf(ParseException.class);
    }

    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(ValidJwkProvider.class)
    void createVerifier(JWK validJwk, String name) {
        assertThat(KeyFactory.createVerifier(validJwk.toPublicJWK())).isNotNull();
    }

    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(ValidJwkProvider.class)
    void createSigner(JWK validJwk, String name) {
        assertThat(KeyFactory.createSigner(validJwk)).isNotNull();
    }

    private static class ValidJwkProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(createEcKey(Curve.P_256), "EC Key with P256 Curve"),
                    Arguments.of(createEcKey(Curve.P_384), "EC Key with P384 Curve"),
                    Arguments.of(createEcKey(Curve.P_521), "EC Key with P512 Curve"),
                    Arguments.of(createOkp(), "Octet Key Pair"),
                    Arguments.of(createRsaKey(2048), "RSA Key, 2048 bit"),
                    Arguments.of(createRsaKey(4096), "RSA Key, 4096 bit")
            );
        }

        private RSAKey createRsaKey(int keysize) {
            try {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                gen.initialize(keysize);
                KeyPair keyPair = gen.generateKeyPair();

                return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                        .privateKey((RSAPrivateKey) keyPair.getPrivate())
                        .keyUse(KeyUse.SIGNATURE)
                        .keyID(UUID.randomUUID().toString())
                        .issueTime(new Date())
                        .build();

            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        private OctetKeyPair createOkp() {
            try {
                return new OctetKeyPairGenerator(Curve.Ed25519)
                        .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key (optional)
                        .keyID(UUID.randomUUID().toString()) // give the key a unique ID (optional)
                        .issueTime(new Date()) // issued-at timestamp (optional)
                        .generate();
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }

        }

        private ECKey createEcKey(Curve curve) {
            try {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
                gen.initialize(curve.toECParameterSpec());
                KeyPair keyPair = gen.generateKeyPair();

                return new ECKey.Builder(curve, (ECPublicKey) keyPair.getPublic())
                        .privateKey((ECPrivateKey) keyPair.getPrivate())
                        .build();
            } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }
}