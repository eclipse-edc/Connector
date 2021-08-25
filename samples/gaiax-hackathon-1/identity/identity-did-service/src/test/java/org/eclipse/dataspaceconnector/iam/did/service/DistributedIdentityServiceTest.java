/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.iam.did.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsResult;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.resolver.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.testFixtures.TemporaryKeyLoader;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 */
class DistributedIdentityServiceTest {
    private DistributedIdentityService identityService;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @Test
    void verifyResolveHubUrl() throws JsonProcessingException {
        @SuppressWarnings("unchecked") var url = identityService.resolveHubUrl(new ObjectMapper().readValue(TestDids.HUB_URL_DID, Map.class));
        Assertions.assertEquals("https://myhub.com", url);
    }

    @Test
    void verifyObtainClientCredentials() throws Exception {
        var result = identityService.obtainClientCredentials("Foo");

        Assertions.assertTrue(result.success());

        var jwt = SignedJWT.parse(result.getToken());
        var verifier = new RSASSAVerifier(publicKey);
        Assertions.assertTrue(jwt.verify(verifier));
    }

    @Test
    void verifyJwtToken() throws Exception {
        var signer = new RSASSASigner(privateKey);

        var expiration = new Date().getTime() + TimeUnit.MINUTES.toMillis(10);
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("foo")
                .issuer("did:ion:123abc")
                .expirationTime(new Date(expiration))
                .build();

        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("primary").build(), claimsSet);
        jwt.sign(signer);

        var token = jwt.serialize();

        var result = identityService.verifyJwtToken(token, "Foo");

        Assertions.assertTrue(result.valid());
        Assertions.assertEquals("EU", result.token().getClaims().get("region"));
    }

    @BeforeEach
    void setUp() {
        privateKey = TemporaryKeyLoader.loadPrivateKey();
        publicKey = TemporaryKeyLoader.loadPublicKey();

        DidResolver didResolver = d -> {
            try {
                //noinspection unchecked
                return new ObjectMapper().readValue(TestDids.HUB_URL_DID, LinkedHashMap.class);
            } catch (JsonProcessingException e) {
                throw new AssertionError(e);
            }
        };

        CredentialsVerifier verifier = (document, url) -> new CredentialsResult(Map.of("region", "EU"));
        identityService = new DistributedIdentityService("did:ion:123abc", verifier, didResolver, d -> publicKey, k -> privateKey, new Monitor() {
        });

    }


}
