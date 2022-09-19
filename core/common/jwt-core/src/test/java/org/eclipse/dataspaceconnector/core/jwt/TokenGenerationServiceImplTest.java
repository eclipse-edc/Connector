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

package org.eclipse.dataspaceconnector.core.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import org.assertj.core.api.Condition;
import org.assertj.core.api.HamcrestCondition;
import org.eclipse.dataspaceconnector.spi.jwt.JwtDecorator;
import org.eclipse.dataspaceconnector.spi.jwt.TokenGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.nimbusds.jose.JWSAlgorithm.RS256;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.PATH;

class TokenGenerationServiceImplTest {

    private RSAKey keys;
    private TokenGenerationService tokenGenerationService;

    @BeforeEach
    void setUp() throws JOSEException {
        keys = testKey();
        tokenGenerationService = new TokenGenerationServiceImpl(keys.toPrivateKey());
    }

    @Test
    void verifyTokenGeneration() throws ParseException, JOSEException {
        var decorator = testDecorator();

        var result = tokenGenerationService.generate(decorator);

        assertThat(result.succeeded()).isTrue();
        var token = result.getContent().getToken();

        // check signature
        var signedJwt = SignedJWT.parse(token);
        assertThat(signedJwt.verify(createVerifier(signedJwt.getHeader(), keys.toPublicKey()))).isTrue();

        // check claims
        var tested = signedJwt.getJWTClaimsSet().getClaims();
        assertThat(tested)
                .containsEntry("foo", "bar")
                .containsKey("exp")
                .extracting(it -> it.get("exp")).isInstanceOf(Date.class);

        assertThat(signedJwt.getHeader()).satisfies(header -> {
            assertThat(header.getAlgorithm()).isEqualTo(RS256);
            assertThat(header.getX509CertThumbprint()).isEqualTo(Base64URL.from("some x509CertThumbprint thing"));
        });
    }

    private JWSVerifier createVerifier(JWSHeader header, Key publicKey) throws JOSEException {
        return new DefaultJWSVerifierFactory().createJWSVerifier(header, publicKey);
    }

    private JwtDecorator testDecorator() {
        return new JwtDecorator() {

            @Override
            public Map<String, Object> claims() {
                return Map.of("foo", "bar", "exp", Date.from(Instant.now().plusSeconds(60)));
            }

            @Override
            public Map<String, Object> headers() {
                return Map.of("x5t", "some x509CertThumbprint thing");
            }
        };
    }

    private static RSAKey testKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }
}