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
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.TokenValidationRulesRegistryImpl;
import org.eclipse.edc.token.TokenValidationServiceImpl;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TokenValidationServiceImplTest {

    private final Instant now = Instant.now();
    private final PublicKeyResolver publicKeyResolver = mock();
    private TokenValidationService tokenValidationService;
    private RSAKey key;
    private String publicKeyId;

    @BeforeEach
    public void setUp() throws JOSEException {
        key = testKey();
        var publicKey = (RSAPublicKey) key.toPublicKey();
        publicKeyId = UUID.randomUUID().toString();

        when(publicKeyResolver.resolveKey(any())).thenReturn(Result.failure("not found"));
        when(publicKeyResolver.resolveKey(eq(publicKeyId))).thenReturn(Result.success(publicKey));
        var rulesRegistry = new TokenValidationRulesRegistryImpl();
        tokenValidationService = new TokenValidationServiceImpl();
    }

    @Test
    void validationSuccess() throws JOSEException {
        var claims = createClaims(now);
        var ruleMock = mock(TokenValidationRule.class);
        when(ruleMock.checkRule(any(), any())).thenReturn(Result.success());

        var result = tokenValidationService.validate(createJwt(publicKeyId, claims, key.toPrivateKey()), publicKeyResolver);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getClaims())
                .containsEntry("foo", "bar")
                .hasEntrySatisfying(EXPIRATION_TIME, value -> assertThat((Date) value).isCloseTo(now, 1000));
    }

    @Test
    void validationFailure_cannotResolvePublicKey() throws JOSEException {
        var claims = createClaims(now);
        var ruleMock = mock(TokenValidationRule.class);

        var result = tokenValidationService.validate(createJwt("unknown-key", claims, key.toPrivateKey()), publicKeyResolver, ruleMock);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("not found");
        verifyNoInteractions(ruleMock);
    }

    @Test
    void validationFailure_singleRuleFails() throws JOSEException {
        var claims = createClaims(now);
        var ruleMock = mock(TokenValidationRule.class);
        when(ruleMock.checkRule(any(), any())).thenReturn(Result.failure("Rule validation failed!"));

        var result = tokenValidationService.validate(createJwt(publicKeyId, claims, key.toPrivateKey()), publicKeyResolver, ruleMock);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("Rule validation failed!");
    }

    @Test
    void validationFailure_multipleRulesFail() throws JOSEException {
        var claims = createClaims(now);
        var r1 = mock(TokenValidationRule.class);
        var r2 = mock(TokenValidationRule.class);
        var r3 = mock(TokenValidationRule.class);

        when(r1.checkRule(any(), any())).thenReturn(Result.failure("test-failure1"));
        when(r2.checkRule(any(), any())).thenReturn(Result.success());
        when(r3.checkRule(any(), any())).thenReturn(Result.failure("test-failure2"));

        var result = tokenValidationService.validate(createJwt(publicKeyId, claims, key.toPrivateKey()), publicKeyResolver, r1, r2, r3);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactlyInAnyOrder("test-failure1", "test-failure2");
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
