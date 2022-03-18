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

package org.eclipse.dataspaceconnector.common.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
                .hasEntrySatisfying("exp", new Condition<>() {
                    public boolean matches(Object value) {
                        return value instanceof Date;
                    }
                });
    }

    private JWSVerifier createVerifier(JWSHeader header, Key publicKey) throws JOSEException {
        return new DefaultJWSVerifierFactory().createJWSVerifier(header, publicKey);
    }

    private JwtDecorator testDecorator() {
        return (header, claimsSet) -> {
            claimsSet.expirationTime(Date.from(Instant.now().plusSeconds(60)))
                    .claim("foo", "bar")
                    .build();
        };
    }

    private static RSAKey testKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }
}