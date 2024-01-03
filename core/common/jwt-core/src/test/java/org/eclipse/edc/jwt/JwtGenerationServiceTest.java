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

package org.eclipse.edc.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.jwt.spi.JwtDecorator;
import org.eclipse.edc.jwt.spi.TokenGenerationService;
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
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;

class JwtGenerationServiceTest {

    private RSAKey keys;
    private TokenGenerationService tokenGenerationService;

    private static RSAKey testKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }

    @BeforeEach
    void setUp() throws JOSEException {
        keys = testKey();
        tokenGenerationService = new JwtGenerationService();
    }

    @Test
    void verifyTokenGeneration() throws ParseException, JOSEException {
        var decorator = testDecorator();

        var result = tokenGenerationService.generate(() -> {
            try {
                return keys.toPrivateKey();
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }
        }, decorator);

        assertThat(result.succeeded()).isTrue();
        var token = result.getContent().getToken();

        // check signature
        var signedJwt = SignedJWT.parse(token);
        assertThat(signedJwt.verify(createVerifier(signedJwt.getHeader(), keys.toPublicKey()))).isTrue();

        // check claims
        var tested = signedJwt.getJWTClaimsSet().getClaims();
        assertThat(tested)
                .containsEntry("foo", "bar")
                .containsKey(EXPIRATION_TIME)
                .hasEntrySatisfying(EXPIRATION_TIME, value -> assertThat(value).isInstanceOf(Date.class));

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
                return Map.of("foo", "bar", EXPIRATION_TIME, Date.from(Instant.now().plusSeconds(60)));
            }

            @Override
            public Map<String, Object> headers() {
                return Map.of("x5t", "some x509CertThumbprint thing");
            }
        };
    }
}