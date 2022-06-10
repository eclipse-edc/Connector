/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core.identity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.common.token.JwtDecoratorRegistryImpl;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationService;
import org.eclipse.dataspaceconnector.common.token.TokenValidationServiceImpl;
import org.eclipse.dataspaceconnector.iam.oauth2.core.Oauth2Configuration;
import org.eclipse.dataspaceconnector.iam.oauth2.core.rule.Oauth2ValidationRulesRegistryImpl;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.testOkHttpClient;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Oauth2ServiceImplTest {

    private static final String TOKEN_URL = "http://test.com";
    private static final String CLIENT_ID = "client-test";
    private static final String PRIVATE_KEY_ALIAS = "pk-test";
    private static final String PUBLIC_CERTIFICATE_ALIAS = "cert-test";
    private static final String PROVIDER_AUDIENCE = "audience-test";

    private Instant now = Instant.now();
    private Oauth2ServiceImpl authService;
    private JWSSigner jwsSigner;

    @BeforeEach
    void setUp() throws JOSEException {
        var testKey = testKey();

        jwsSigner = new RSASSASigner(testKey.toPrivateKey());
        var publicKeyResolverMock = mock(PublicKeyResolver.class);
        var privateKeyResolverMock = mock(PrivateKeyResolver.class);
        var certificateResolverMock = mock(CertificateResolver.class);
        when(publicKeyResolverMock.resolveKey(anyString())).thenReturn(testKey.toPublicKey());
        var configuration = Oauth2Configuration.Builder.newInstance()
                .tokenUrl(TOKEN_URL)
                .clientId(CLIENT_ID)
                .privateKeyAlias(PRIVATE_KEY_ALIAS)
                .publicCertificateAlias(PUBLIC_CERTIFICATE_ALIAS)
                .providerAudience(PROVIDER_AUDIENCE)
                .privateKeyResolver(privateKeyResolverMock)
                .certificateResolver(certificateResolverMock)
                .identityProviderKeyResolver(publicKeyResolverMock)
                .build();

        var clock = Clock.fixed(now, UTC);
        var validationRulesRegistry = new Oauth2ValidationRulesRegistryImpl(configuration, clock);
        var tokenValidationService = new TokenValidationServiceImpl(publicKeyResolverMock, validationRulesRegistry);

        authService = new Oauth2ServiceImpl(configuration, mock(TokenGenerationService.class), testOkHttpClient(), new JwtDecoratorRegistryImpl(), new TypeManager(), tokenValidationService);
    }

    @Test
    void verifyNoAudienceToken() {
        var jwt = createJwt(null, Date.from(now.minusSeconds(1000)), Date.from(now.plusSeconds(1000)));

        var result = authService.verifyJwtToken(jwt.serialize());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).isNotEmpty();
    }

    @Test
    void verifyInvalidAudienceToken() {
        var jwt = createJwt("different.audience", Date.from(now.minusSeconds(1000)), Date.from(now.plusSeconds(1000)));

        var result = authService.verifyJwtToken(jwt.serialize());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).isNotEmpty();
    }

    @Test
    void verifyInvalidAttemptUseNotBeforeToken() {
        var jwt = createJwt(PROVIDER_AUDIENCE, Date.from(now.plusSeconds(1000)), Date.from(now.plusSeconds(1000)));

        var result = authService.verifyJwtToken(jwt.serialize());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).isNotEmpty();
    }

    @Test
    void verifyExpiredToken() {
        var jwt = createJwt(PROVIDER_AUDIENCE, Date.from(now.minusSeconds(1000)), Date.from(now.minusSeconds(1000)));

        var result = authService.verifyJwtToken(jwt.serialize());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).isNotEmpty();
    }

    @Test
    void verifyValidJwt() {
        var jwt = createJwt(PROVIDER_AUDIENCE, Date.from(now.minusSeconds(1000)), new Date(System.currentTimeMillis() + 1000000));

        var result = authService.verifyJwtToken(jwt.serialize());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getClaims()).hasSize(3).containsKeys("aud", "nbf", "exp");
    }

    private RSAKey testKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }

    private SignedJWT createJwt(String aud, Date nbf, Date exp) {
        var claimsSet = new JWTClaimsSet.Builder()
                .audience(aud)
                .notBeforeTime(nbf)
                .expirationTime(exp).build();
        var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("an-id").build();

        try {
            SignedJWT jwt = new SignedJWT(header, claimsSet);
            jwt.sign(jwsSigner);
            return jwt;
        } catch (JOSEException e) {
            throw new AssertionError(e);
        }
    }
}
