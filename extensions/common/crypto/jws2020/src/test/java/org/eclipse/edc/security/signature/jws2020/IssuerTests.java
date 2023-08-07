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

import com.apicatalog.jsonld.loader.SchemeRouter;
import com.apicatalog.ld.DocumentError;
import com.apicatalog.ld.signature.SigningError;
import com.apicatalog.vc.Vc;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.security.signature.jws2020.TestFunctions.createKeyPair;
import static org.eclipse.edc.security.signature.jws2020.TestFunctions.readResourceAsJson;
import static org.eclipse.edc.security.signature.jws2020.TestFunctions.readResourceAsString;

class IssuerTests {

    private final JwsSignature2020Suite jws2020suite = new JwsSignature2020Suite(JacksonJsonLd.createObjectMapper());
    //used to load remote data from a local directory
    private final TestResourcesLoader loader = new TestResourcesLoader("https://org.eclipse.edc/", "jws2020/issuing/", SchemeRouter.defaultInstance());

    @DisplayName("t0001: a simple credential to sign (EC Key)")
    @Test
    void signSimpleCredential_ecKey() throws SigningError, DocumentError {
        var vc = readResourceAsJson("jws2020/issuing/0001_vc.json");
        var keypair = createKeyPair(KeyFactory.create(readResourceAsString("jws2020/issuing/private-key.json")));

        var verificationMethodUrl = "https://org.eclipse.edc/verification-method";

        var proofOptions = jws2020suite.createOptions()
                .created(Instant.parse("2022-12-31T23:00:00Z"))
                .verificationMethod(new JwkMethod(URI.create(verificationMethodUrl), null, null, null))
                .purpose(URI.create("https://w3id.org/security#assertionMethod"));


        var issuer = Vc.sign(vc, keypair, proofOptions).loader(loader);

        // would throw an exception
        var compacted = IssuerCompat.compact(issuer, "https://www.w3.org/ns/did/v1");
        var verificationMethod = compacted.getJsonObject("sec:proof").get("verificationMethod");

        assertThat(verificationMethod).describedAs("Expected a String!").isInstanceOf(JsonString.class);
        assertThat(((JsonString) verificationMethod).getString()).isEqualTo(verificationMethodUrl);
    }

    @DisplayName("t0001: a simple credential to sign (RSA Key)")
    @ParameterizedTest(name = "keySize = {0} bits")
    @ValueSource(ints = { 2048, 3072, 4096 })
    void signSimpleCredential_rsaKey(int keysize) throws SigningError, DocumentError, NoSuchAlgorithmException {
        var vc = readResourceAsJson("jws2020/issuing/0001_vc.json");

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(keysize);
        var keyPair = gen.generateKeyPair();

        var jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .build();
        var keypair = createKeyPair(jwk);

        var verificationMethodUrl = "https://org.eclipse.edc/verification-method";

        var proofOptions = jws2020suite.createOptions()
                .created(Instant.parse("2022-12-31T23:00:00Z"))
                .verificationMethod(new JwkMethod(URI.create(verificationMethodUrl), null, null, null))
                .purpose(URI.create("https://w3id.org/security#assertionMethod"));


        var issuer = Vc.sign(vc, keypair, proofOptions).loader(loader);

        // would throw an exception
        var compacted = IssuerCompat.compact(issuer, "https://www.w3.org/ns/did/v1");
        var verificationMethod = compacted.getJsonObject("sec:proof").get("verificationMethod");

        assertThat(verificationMethod).describedAs("Expected a String!").isInstanceOf(JsonString.class);
        assertThat(((JsonString) verificationMethod).getString()).isEqualTo(verificationMethodUrl);
    }

    @DisplayName("t0001: a simple credential to sign (OctetKeyPair)")
    @Test
    void signSimpleCredential_octetKeyPair() throws SigningError, DocumentError, JOSEException {
        var vc = readResourceAsJson("jws2020/issuing/0001_vc.json");

        var jwk = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        var keypair = createKeyPair(jwk);

        var verificationMethodUrl = "https://org.eclipse.edc/verification-method";

        var proofOptions = jws2020suite.createOptions()
                .created(Instant.parse("2022-12-31T23:00:00Z"))
                .verificationMethod(new JwkMethod(URI.create(verificationMethodUrl), null, null, null))
                .purpose(URI.create("https://w3id.org/security#assertionMethod"));


        var issuer = Vc.sign(vc, keypair, proofOptions).loader(loader);

        // would throw an exception
        var compacted = IssuerCompat.compact(issuer, "https://www.w3.org/ns/did/v1");
        var verificationMethod = compacted.getJsonObject("sec:proof").get("verificationMethod");

        assertThat(verificationMethod).describedAs("Expected a String!").isInstanceOf(JsonString.class);
        assertThat(((JsonString) verificationMethod).getString()).isEqualTo(verificationMethodUrl);
    }

    @DisplayName("t0002: compacted signed credential")
    @Test
    void signCompactedCredential() {
        // nothing to do here, it's the same as above
    }

    @DisplayName("t0003: signed embedded verificationMethod")
    @Test
    void signEmbeddedVerificationMethod() throws SigningError, DocumentError {
        var vc = readResourceAsJson("jws2020/issuing/0001_vc.json");
        var keypair = createKeyPair(KeyFactory.create(readResourceAsString("jws2020/issuing/private-key.json")));

        var proofOptions = jws2020suite.createOptions()
                .created(Instant.parse("2022-12-31T23:00:00Z"))
                .verificationMethod(keypair)
                .purpose(URI.create("https://w3id.org/security#assertionMethod"));


        var issuer = Vc.sign(vc, keypair, proofOptions).loader(loader);

        // would throw an exception
        var compacted = IssuerCompat.compact(issuer, "https://www.w3.org/ns/did/v1");
        var verificationMethod = compacted.getJsonObject("sec:proof").get("verificationMethod");

        assertThat(verificationMethod).describedAs("Expected an Object!").isInstanceOf(JsonObject.class);
        assertThat(verificationMethod.asJsonObject().get("publicKeyJwk"))
                .isInstanceOf(JsonObject.class)
                .satisfies(jv -> {
                    assertThat(jv.asJsonObject().get("x")).isNotNull();
                    assertThat(jv.asJsonObject().get("crv")).isNotNull();
                    assertThat(jv.asJsonObject().get("kty")).isNotNull();
                });
    }

    @DisplayName("t0004: a credential with DID key as verification method")
    @Test
    void signVerificationDidKey() throws SigningError, DocumentError {
        var vc = readResourceAsJson("jws2020/issuing/0001_vc.json");
        var eckey = (ECKey) KeyFactory.create("""
                {
                    "kty": "EC",
                    "d": "UEUJVbKZC3vR-y65gXx8NZVnE0QD5xe6qOk4eiObj-qVOg5zqt9zc0d6fdu4mUuu",
                    "use": "sig",
                    "crv": "P-384",
                    "x": "l6IS348kIFEANYl3CWueMYVXcZmK0eMI0vejkF1GHbl77dOZuZwi9L2IQmuA27ux",
                    "y": "m-8s5FM8Tn00OKVFxE-wfCs3J2keE2EBAYYZgAmfI1LCRD9iU2LBced-EBK18Da9",
                    "alg": "ES384"
                }
                """);
        var keypair = createKeyPair(eckey);

        // check https://w3c-ccg.github.io/did-method-key/#create for details
        var didKey = "did:key:zC2zU1wUHhYYX4CDwNwky9f5jtSvp5aQy5aNRQMHEdpK5xkJMy6TcMbWBP3scHbR6hhidR3RRjfAA7cuLxjydXgEiZUzRzguozYFeR3G6SzjAwswJ6hXKBWhFEHm2L6Rd6GRAw8r3kyPovxvcabdMF2gBy5TAioY1mVYFeT6";

        var proofOptions = jws2020suite.createOptions()
                .created(Instant.parse("2022-12-31T23:00:00Z"))
                .verificationMethod(new JwkMethod(URI.create(didKey), null, null, null))
                .purpose(URI.create("https://w3id.org/security#assertionMethod"));


        var issuer = Vc.sign(vc, keypair, proofOptions).loader(loader);

        // would throw an exception
        var compacted = IssuerCompat.compact(issuer, "https://www.w3.org/ns/did/v1");
        var verificationMethod = compacted.getJsonObject("sec:proof").get("verificationMethod");

        assertThat(verificationMethod).describedAs("Expected a String!").isInstanceOf(JsonString.class);
        assertThat(((JsonString) verificationMethod).getString()).isEqualTo(didKey);

    }

    @DisplayName("t0005: compacted signed presentation")
    @Test
    void signCompactedPresentation() throws SigningError, DocumentError {
        var vp = readResourceAsJson("jws2020/issuing/0005_vp_compacted_signed.json");

        var keypair = createKeyPair(KeyFactory.create(readResourceAsString("jws2020/issuing/private-key.json")));

        var verificationMethodUrl = "https://org.eclipse.edc/verification-method";

        var proofOptions = jws2020suite.createOptions()
                .created(Instant.parse("2022-12-31T23:00:00Z"))
                .verificationMethod(new JwkMethod(URI.create(verificationMethodUrl), null, null, null))
                .purpose(URI.create("https://w3id.org/security#assertionMethod"));


        var issuer = Vc.sign(vp, keypair, proofOptions).loader(loader);

        // would throw an exception
        var compacted = IssuerCompat.compact(issuer, "https://www.w3.org/ns/did/v1");
        var verificationMethod = compacted.getJsonObject("sec:proof").get("verificationMethod");

        assertThat(verificationMethod).describedAs("Expected a String!").isInstanceOf(JsonString.class);
        assertThat(((JsonString) verificationMethod).getString()).isEqualTo(verificationMethodUrl);
    }

}
