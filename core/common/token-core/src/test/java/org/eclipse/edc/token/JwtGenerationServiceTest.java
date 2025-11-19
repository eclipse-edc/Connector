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

package org.eclipse.edc.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static com.nimbusds.jose.JWSAlgorithm.RS256;
import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static org.assertj.core.api.Assertions.assertThat;

class JwtGenerationServiceTest {

    public static final String TEST_KEY_ID = "test-key-id";
    private RSAKey keys;
    private JwtGenerationService tokenGenerationService;

    @BeforeEach
    void setUp() throws JOSEException {
        keys = testKey();
        tokenGenerationService = new JwtGenerationService(new JwsSignerProvider() {
            @Override
            public Result<JWSSigner> createJwsSigner(String privateKeyId) {
                throw new AssertionError("deprecated method: should not be used anymore");
            }

            @Override
            public Result<JWSSigner> createJwsSigner(String participantContextId, String privateKeyId) {
                if (TEST_KEY_ID.equals(privateKeyId)) {
                    try {
                        var pk = keys.toPrivateKey();
                        return Result.success(CryptoConverter.createSignerFor(pk));
                    } catch (JOSEException e) {
                        return Result.failure(e.getMessage());
                    }
                }
                return Result.failure("key not found");
            }
        });
    }

    @Test
    void verifyTokenGeneration() throws ParseException, JOSEException {
        var decorator = testDecorator();

        var result = tokenGenerationService.generate("test-participant", TEST_KEY_ID, decorator);

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

    @Test
    void shouldFail_whenPrivateKeyCannotBeResolved() {
        var decorator = testDecorator();

        var result = tokenGenerationService.generate("test-participant", "not-exist-key", decorator);

        assertThat(result.failed()).isTrue();
    }

    private JWSVerifier createVerifier(JWSHeader header, Key publicKey) throws JOSEException {
        return new DefaultJWSVerifierFactory().createJWSVerifier(header, publicKey);
    }

    private RSAKey testKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }

    private TokenDecorator testDecorator() {
        return (tokenParameters) -> tokenParameters.claims("foo", "bar")
                .claims(EXPIRATION_TIME, Date.from(Instant.now().plusSeconds(60)))
                .header("x5t", "some x509CertThumbprint thing");
    }
}
