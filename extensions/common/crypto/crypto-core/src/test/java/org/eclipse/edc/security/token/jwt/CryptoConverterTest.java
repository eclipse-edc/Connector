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

package org.eclipse.edc.security.token.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyConverter;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Named;
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
import java.security.Provider;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;

class CryptoConverterTest {

    private static KeyPair createEd25519(@Nullable Provider provider) throws NoSuchAlgorithmException {

        KeyPairGenerator kpg = provider == null ?
                KeyPairGenerator.getInstance("Ed25519") :
                KeyPairGenerator.getInstance("Ed25519", provider);
        return kpg.generateKeyPair();
    }

    private static KeyPair createEc() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        var gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
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

        assertThat(CryptoConverter.createSignerFor(pk.getPrivate())).isInstanceOf(RSASSASigner.class);
    }

    @Test
    void createSignerFor_ecKey() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        var pk = createEc();

        assertThat(CryptoConverter.createSignerFor(pk.getPrivate())).isInstanceOf(ECDSASigner.class);
    }

    @Test
    void createSignerFor_edDsaKey_sunProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(null);
        assertThat(CryptoConverter.createSignerFor(kp.getPrivate())).isInstanceOf(Ed25519Signer.class);
    }

    @Test
    void createSignerFor_edDsaKey_bouncyCastleProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(new BouncyCastleProvider());
        assertThat(CryptoConverter.createSignerFor(kp.getPrivate())).isInstanceOf(Ed25519Signer.class);
    }

    @Test
    void createVerifierFor_rsaKey() throws NoSuchAlgorithmException {
        var pk = createRsa().getPublic();
        assertThat(CryptoConverter.createVerifierFor(pk)).isInstanceOf(RSASSAVerifier.class);
    }

    @Test
    void createVerifierFor_ecKey() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        var pk = createEc().getPublic();
        assertThat(CryptoConverter.createVerifierFor(pk)).isInstanceOf(ECDSAVerifier.class);
    }

    @Test
    void createVerifierFor_edDsaKey_sunProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(null);
        assertThat(CryptoConverter.createVerifierFor(kp.getPublic())).isInstanceOf(Ed25519Verifier.class);
    }

    @Test
    void createVerifierFor_edDsaKey_bouncyCastleProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(new BouncyCastleProvider());
        assertThat(CryptoConverter.createVerifierFor(kp.getPublic())).isInstanceOf(Ed25519Verifier.class);
    }

    @Test
    void convertToJwk_rsaKey() throws NoSuchAlgorithmException {
        var pk = createRsa();
        var jwk = CryptoConverter.createJwk(pk);
        assertThat(jwk).isInstanceOf(RSAKey.class);
        assertThat(jwk.isPrivate()).isTrue();
        assertThat(jwk.getKeyID()).isNull();
    }

    @Test
    void convertToJwk_ecKey_fromPublic() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        var pk = createEc();
        var jwk = CryptoConverter.createJwk(new KeyPair(pk.getPublic(), null));
        assertThat(jwk).isInstanceOf(ECKey.class);
        assertThat(jwk.isPrivate()).isFalse();
        assertThat(jwk.getKeyID()).isNull();
        assertThat(KeyConverter.toJavaKeys(List.of(jwk))).containsExactlyInAnyOrder(pk.getPublic());
    }

    @Test
    void convertToJwk_ecKey_fromPrivate() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var pk = createEc();

        var jwk2 = CryptoConverter.createJwk(new KeyPair(null, pk.getPrivate()));
        assertThat(jwk2).isInstanceOf(ECKey.class);
        assertThat(jwk2.isPrivate()).isTrue();
        assertThat(jwk2.getKeyID()).isNull();
        assertThat(KeyConverter.toJavaKeys(List.of(jwk2))).containsExactlyInAnyOrder(pk.getPublic(), pk.getPrivate());
    }

    @Test
    void convertToJwk_ecKey_fromKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var pk = createEc();
        var jwk3 = CryptoConverter.createJwk(new KeyPair(pk.getPublic(), pk.getPrivate()));
        assertThat(jwk3).isInstanceOf(ECKey.class);
        assertThat(jwk3.isPrivate()).isTrue();
        assertThat(jwk3.getKeyID()).isNull();
        assertThat(KeyConverter.toJavaKeys(List.of(jwk3))).containsExactlyInAnyOrder(pk.getPublic(), pk.getPrivate());

    }

    @Test
    void convertToJwk_edDsaKey_fromPrivate_sunProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(null);
        var jwk = CryptoConverter.createJwk(new KeyPair(null, kp.getPrivate()));
        assertThat(jwk).isInstanceOf(OctetKeyPair.class);
        assertThat(jwk.isPrivate()).isTrue();
        assertThat(jwk.toPublicJWK()).isNotNull();
        assertThat(jwk.getKeyID()).isNull();
    }

    @Test
    void convertToJwk_edDsaKey_fromPublic_sunProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(null);
        var jwk = CryptoConverter.createJwk(new KeyPair(kp.getPublic(), null));

        assertThat(jwk).isInstanceOf(OctetKeyPair.class);
        assertThat(jwk.isPrivate()).isFalse();
        assertThat(jwk.toPublicJWK()).isNotNull();
        assertThat(jwk.toPublicJWK()).isEqualTo(jwk);
        assertThat(jwk.getKeyID()).isNull();

    }

    @Test
    void convertToJwk_edDsaKey_sunProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(null);
        var jwk = CryptoConverter.createJwk(new KeyPair(kp.getPublic(), kp.getPrivate()));

        assertThat(jwk).isInstanceOf(OctetKeyPair.class);
        assertThat(jwk.isPrivate()).isTrue();
        assertThat(jwk.toPublicJWK()).isNotNull();
        assertThat(jwk.getKeyID()).isNull();
    }

    @Test
    void convertToJwk_edDsaKey_fromPrivate_bouncyCastleProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(new BouncyCastleProvider());
        var jwk = CryptoConverter.createJwk(new KeyPair(null, kp.getPrivate()));
        assertThat(jwk).isInstanceOf(OctetKeyPair.class);
        assertThat(jwk.isPrivate()).isTrue();
        assertThat(jwk.toPublicJWK()).isNotNull();
        assertThat(jwk.getKeyID()).isNull();
    }

    @Test
    void convertToJwk_edDsaKey_fromPublic_bouncyCastleProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(new BouncyCastleProvider());
        var jwk = CryptoConverter.createJwk(new KeyPair(kp.getPublic(), null));

        assertThat(jwk).isInstanceOf(OctetKeyPair.class);
        assertThat(jwk.isPrivate()).isFalse();
        assertThat(jwk.toPublicJWK()).isNotNull();
        assertThat(jwk.toPublicJWK()).isEqualTo(jwk);
        assertThat(jwk.getKeyID()).isNull();

    }

    @Test
    void convertToJwk_edDsaKey_bouncyCastleProvider() throws NoSuchAlgorithmException {
        var kp = createEd25519(new BouncyCastleProvider());
        var jwk = CryptoConverter.createJwk(new KeyPair(kp.getPublic(), kp.getPrivate()));

        assertThat(jwk).isInstanceOf(OctetKeyPair.class);
        assertThat(jwk.isPrivate()).isTrue();
        assertThat(jwk.toPublicJWK()).isNotNull();
        assertThat(jwk.getKeyID()).isNull();
    }


    @ParameterizedTest
    @ArgumentsSource(KeyProvider.class)
    void getJwsAlgorithm(KeyPair keypair) {
        var signer = CryptoConverter.createSignerFor(keypair.getPrivate());
        assertThat(CryptoConverter.getRecommendedAlgorithm(signer)).isNotNull();
    }


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

        assertThat(CryptoConverter.create(json)).isInstanceOf(ECKey.class).extracting(JWK::isPrivate).isEqualTo(true);
        var map = new ObjectMapper().readValue(json, Map.class);
        assertThat(CryptoConverter.create(map)).isInstanceOf(ECKey.class).extracting(JWK::isPrivate).isEqualTo(true);
    }

    @Test
    void create_rsa() throws JsonProcessingException {
        // the RSA key would violate the Checkstyle line length constraint
        var json = getResourceFileContentAsString("rsakey.json");

        assertThat(CryptoConverter.create(json)).isInstanceOf(RSAKey.class).extracting(JWK::isPrivate).isEqualTo(true);
        var map = new ObjectMapper().readValue(json, Map.class);
        assertThat(CryptoConverter.create(map)).isInstanceOf(RSAKey.class).extracting(JWK::isPrivate).isEqualTo(true);
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

        assertThat(CryptoConverter.create(json)).isInstanceOf(OctetKeyPair.class).extracting(JWK::isPrivate).isEqualTo(true);
        var map = new ObjectMapper().readValue(json, Map.class);
        assertThat(CryptoConverter.create(map)).isInstanceOf(OctetKeyPair.class).extracting(JWK::isPrivate).isEqualTo(true);
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

        assertThatThrownBy(() -> assertThat(CryptoConverter.create(json))).isInstanceOf(RuntimeException.class).rootCause().isInstanceOf(ParseException.class);

        var map = new ObjectMapper().readValue(json, Map.class);
        assertThatThrownBy(() -> assertThat(CryptoConverter.create(map))).isInstanceOf(RuntimeException.class).rootCause().isInstanceOf(ParseException.class);
    }

    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(ValidJwkProvider.class)
    void createVerifier(JWK validJwk, String name) {
        assertThat(CryptoConverter.createVerifier(validJwk.toPublicJWK())).isNotNull();
    }

    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(ValidJwkProvider.class)
    void createSigner(JWK validJwk, String name) {
        assertThat(CryptoConverter.createSigner(validJwk)).isNotNull();
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