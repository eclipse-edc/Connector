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

package org.eclipse.dataspaceconnector.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenGenerationServiceImplTest {

    private TokenGenerationService tokenGenerationService;

    @BeforeEach
    void setUp() throws JOSEException {
        var testKey = testKey();
        var jwsSigner = new RSASSASigner(testKey.toPrivateKey());
        tokenGenerationService = new TokenGenerationServiceImpl(jwsSigner);
    }

    @Test
    void test() throws ParseException {
        var exp = Date.from(Instant.now().plusSeconds(100));
        var result = tokenGenerationService.generate(createClaims(exp));

        assertThat(result.succeeded()).isTrue();
        var signedJwt = SignedJWT.parse(result.getContent().getToken());

        var claims = signedJwt.getJWTClaimsSet().getClaims();
        assertThat(claims)
                .containsEntry("foo", "bar")
                .containsEntry("exp", Date.from(exp.toInstant().truncatedTo(ChronoUnit.SECONDS)));
    }

    private static Map<String, Object> createClaims(Date exp) {
        return Map.of("exp", exp, "foo", "bar");
    }

    private static RSAKey testKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }
}