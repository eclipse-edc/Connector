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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.jwt.spi.TokenValidationRule;
import org.eclipse.edc.jwt.spi.TokenValidationService;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenValidationServiceImplTest {

    private final Instant now = Instant.now();
    private TokenValidationService tokenValidationService;
    private RSAKey key;
    private TokenValidationRule ruleMock;
    private String publicKeyId;

    @BeforeEach
    public void setUp() throws JOSEException {
        key = testKey();
        ruleMock = mock(TokenValidationRule.class);
        var publicKey = (RSAPublicKey) key.toPublicKey();
        publicKeyId = UUID.randomUUID().toString();
        var resolver = new PublicKeyResolver() {
            @Override
            public Result<PublicKey> resolveKey(String id) {
                return id.equals(publicKeyId) ? Result.success(publicKey) : Result.failure("not found");
            }
        };
        var rulesRegistry = new TokenValidationRulesRegistryImpl();
        rulesRegistry.addRule(ruleMock);
        tokenValidationService = new TokenValidationServiceImpl(resolver, rulesRegistry);
    }

    @Test
    void validationSuccess() throws JOSEException {
        var claims = createClaims(now);
        when(ruleMock.checkRule(any(), any())).thenReturn(Result.success());

        var result = tokenValidationService.validate(createJwt(publicKeyId, claims, key.toPrivateKey()));

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getClaims())
                .containsEntry("foo", "bar")
                .hasEntrySatisfying(EXPIRATION_TIME, value -> assertThat((Date) value).isCloseTo(now, 1000));
    }

    @Test
    void validationFailure_cannotResolvePublicKey() throws JOSEException {
        var claims = createClaims(now);
        when(ruleMock.checkRule(any(), any())).thenReturn(Result.failure("Rule validation failed!"));

        var result = tokenValidationService.validate(createJwt("unknown-key", claims, key.toPrivateKey()));

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("Failed to resolve public key with id: unknown-key");
    }

    @Test
    void validationFailure_validationRuleKo() throws JOSEException {
        var claims = createClaims(now);
        when(ruleMock.checkRule(any(), any())).thenReturn(Result.failure("Rule validation failed!"));

        var result = tokenValidationService.validate(createJwt(publicKeyId, claims, key.toPrivateKey()));

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("Rule validation failed!");
    }

    private String createJwt(String publicKeyId, JWTClaimsSet claimsSet, PrivateKey pk) {
        var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(publicKeyId).build();
        try {
            SignedJWT jwt = new SignedJWT(header, claimsSet);
            jwt.sign(new RSASSASigner(pk));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new AssertionError(e);
        }
    }

    private RSAKey testKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }

    private JWTClaimsSet createClaims(Instant exp) {
        return new JWTClaimsSet.Builder()
                .claim("foo", "bar")
                .expirationTime(Date.from(exp))
                .build();
    }
}
