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
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core.impl;

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
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;

class Oauth2ServiceImplTest {

    private static final String TOKEN_URL = "http://test.com";
    private static final String CLIENT_ID = "client-test";
    private static final String PRIVATE_KEY_ALIAS = "pk-test";
    private static final String PUBLIC_CERTIFICATE_ALIAS = "cert-test";
    private static final String PROVIDER_AUDIENCE = "audience-test";

    private Oauth2ServiceImpl authService;
    private Supplier<JWSSigner> jwsSignerSupplier;

    @Test
    void verifyNoAudienceToken() {
        var jwt = createJwt(null, Date.from(Instant.now().minusSeconds(1000)), Date.from(Instant.now().plusSeconds(1000)));

        var result = authService.verifyJwtToken(jwt.serialize(), "test.audience");

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailures()).isNotEmpty();
    }

    @Test
    void verifyInvalidAudienceToken() {
        var jwt = createJwt("different.audience", Date.from(Instant.now().minusSeconds(1000)), Date.from(Instant.now().plusSeconds(1000)));
        var result = authService.verifyJwtToken(jwt.serialize(), "test.audience");

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailures()).isNotEmpty();
    }

    @Test
    void verifyInvalidAttemptUseNotBeforeToken() {
        var jwt = createJwt("test.audience", Date.from(Instant.now().plusSeconds(1000)), Date.from(Instant.now().plusSeconds(1000)));
        var result = authService.verifyJwtToken(jwt.serialize(), "test.audience");

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailures()).isNotEmpty();
    }

    @Test
    void verifyExpiredToken() {
        var jwt = createJwt("test.audience", Date.from(Instant.now().minusSeconds(1000)), Date.from(Instant.now().minusSeconds(1000)));
        var result = authService.verifyJwtToken(jwt.serialize(), "test.audience");

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailures()).isNotEmpty();
    }

    @Test
    void verifyValidJwt() {
        var jwt = createJwt("test.audience", Date.from(Instant.now().minusSeconds(1000)), new Date(System.currentTimeMillis() + 1000000));
        var result = authService.verifyJwtToken(jwt.serialize(), "test.audience");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getClaims()).hasSize(3).containsKeys("aud", "nbf", "exp");
    }

    @BeforeEach
    void setUp() throws JOSEException {
        RSAKey testKey = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();

        // prepare RS keys for testing
        var pk = testKey.toPrivateKey();
        jwsSignerSupplier = () -> new RSASSASigner(pk);
        //set up a resolver that resolves that test key
        PublicKeyResolver publicKeyResolverMock = niceMock(PublicKeyResolver.class);
        PrivateKeyResolver privateKeyResolverMock = niceMock(PrivateKeyResolver.class);
        CertificateResolver certificateResolverMock = niceMock(CertificateResolver.class);
        expect(publicKeyResolverMock.resolveKey(anyString())).andReturn((RSAPublicKey) testKey.toPublicKey());
        Oauth2Configuration configuration = Oauth2Configuration.Builder.newInstance()
                .tokenUrl(TOKEN_URL)
                .clientId(CLIENT_ID)
                .privateKeyAlias(PRIVATE_KEY_ALIAS)
                .publicCertificateAlias(PUBLIC_CERTIFICATE_ALIAS)
                .providerAudience(PROVIDER_AUDIENCE)
                .privateKeyResolver(privateKeyResolverMock)
                .certificateResolver(certificateResolverMock)
                .identityProviderKeyResolver(publicKeyResolverMock)
                .build();

        replay(publicKeyResolverMock);

        authService = new Oauth2ServiceImpl(configuration, jwsSignerSupplier, new OkHttpClient.Builder().build(), new JwtDecoratorRegistryImpl(), new TypeManager());
    }

    private SignedJWT createJwt(String aud, Date nbf, Date exp) {
        var claimsSet = new JWTClaimsSet.Builder()
                .audience(aud)
                .notBeforeTime(nbf)
                .expirationTime(exp).build();
        var header = new JWSHeader.Builder(JWSAlgorithm.RS256).build();

        try {
            SignedJWT jwt = new SignedJWT(header, claimsSet);
            jwt.sign(jwsSignerSupplier.get());
            return jwt;
        } catch (JOSEException e) {
            throw new AssertionError(e);
        }
    }
}
