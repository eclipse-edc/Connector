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

package org.eclipse.dataspaceconnector.iam.oauth2.impl;

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
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;

class Oauth2ServiceImplTest {
    private Oauth2ServiceImpl authService;
    private Supplier<JWSSigner> jwsSignerSupplier;

    @Test
    void verifyNoAudienceToken() {
        var jwt = createJwt(null, new Date(System.currentTimeMillis() - 10000000), new Date(System.currentTimeMillis() + 10000000));

        var result = authService.verifyJwtToken(jwt.serialize(), "test.audience");
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void verifyInvalidAudienceToken() {
        var jwt = createJwt("different.audience", new Date(System.currentTimeMillis() - 10000000), new Date(System.currentTimeMillis() + 10000000));
        var result = authService.verifyJwtToken(jwt.serialize(), "test.audience");
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void verifyInvalidAttemptUseNotBeforeToken() {
        var jwt = createJwt("test.audience", new Date(System.currentTimeMillis() + 10000000), new Date(System.currentTimeMillis() + 10000000));
        var result = authService.verifyJwtToken(jwt.serialize(), "test.audience");
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void verifyExpiredToken() {

        var jwt = createJwt("test.audience", new Date(System.currentTimeMillis() - 10000000), new Date(System.currentTimeMillis() - 10000000));

        var result = authService.verifyJwtToken(jwt.serialize(), "test.audience");
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void verifyValidJwt() {

        var jwt = createJwt("test.audience", new Date(System.currentTimeMillis() - 1000), new Date(System.currentTimeMillis() + 1000));
        var result = authService.verifyJwtToken(jwt.serialize(), "test.audience");
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isNotNull().isEmpty();
        assertThat(result.token().getClaims()).hasSize(3).containsKeys("aud", "nbf", "exp");
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
        CertificateResolver certResolverMock = niceMock(CertificateResolver.class);
        X509Certificate certMock = niceMock(X509Certificate.class);
        expect(certMock.getPublicKey()).andReturn(testKey.toPublicKey());
        expect(certResolverMock.resolveCertificate(anyString())).andReturn(certMock);
        replay(certMock, certResolverMock);

        authService = new Oauth2ServiceImpl(Oauth2Configuration.Builder.newInstance()
                .certificateResolver(certResolverMock)
                .build(), jwsSignerSupplier);
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
