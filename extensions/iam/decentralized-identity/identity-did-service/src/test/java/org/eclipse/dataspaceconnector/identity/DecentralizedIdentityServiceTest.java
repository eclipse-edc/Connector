/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.identity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.KeyPairFactory;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.EllipticCurvePublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.document.VerificationMethod;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.TokenParameters;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;

import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the {@link DecentralizedIdentityService} with a key algorithm.
 * See {@link WithP256Test} for concrete impl.
 */

abstract class DecentralizedIdentityServiceTest {
    private static final Faker FAKER = new Faker();
    private static final String DID_DOCUMENT = getResourceFileContentAsString("dids.json");

    String didUrl = FAKER.internet().url();
    private DecentralizedIdentityService identityService;

    @Test
    void verifyResolveHubUrl() throws IOException {
        var url = identityService.getHubUrl(new ObjectMapper().readValue(DID_DOCUMENT, DidDocument.class));
        assertEquals("https://myhub.com", url);
    }

    @Test
    void generateAndVerifyJwtToken_valid() {
        var result = identityService.obtainClientCredentials(TokenParameters.Builder.newInstance()
                .scope("Foo")
                .audience("Bar")
                .build());
        assertTrue(result.succeeded());

        Result<ClaimToken> verificationResult = identityService.verifyJwtToken(result.getContent(), "Bar");
        assertTrue(verificationResult.succeeded());
        assertEquals("eu", verificationResult.getContent().getClaims().get("region"));
    }

    @Test
    void generateAndVerifyJwtToken_wrongAudience() {
        var result = identityService.obtainClientCredentials(TokenParameters.Builder.newInstance()
                .scope("Foo")
                .audience("Bar")
                .build());

        Result<ClaimToken> verificationResult = identityService.verifyJwtToken(result.getContent(), "Bar2");
        assertTrue(verificationResult.failed());
    }

    @BeforeEach
    void setUp() {
        var keyPair = getKeyPair();
        var privateKey = new EcPrivateKeyWrapper(keyPair.toECKey());

        var didResolver = new TestResolverRegistry(DID_DOCUMENT, keyPair);
        CredentialsVerifier verifier = document -> Result.success(Map.of("region", "eu"));
        identityService = new DecentralizedIdentityService(didResolver, verifier, new ConsoleMonitor(), privateKey, didUrl, Clock.systemUTC());
    }

    @NotNull
    protected abstract JWK getKeyPair();

    public static class WithP256Test extends DecentralizedIdentityServiceTest {
        @Override
        protected @NotNull ECKey getKeyPair() {
            return KeyPairFactory.generateKeyPairP256();
        }

    }

    private static class TestResolverRegistry implements DidResolverRegistry {
        private final String hubUrlDid;
        private final JWK keyPair;

        TestResolverRegistry(String hubUrlDid, JWK keyPair) {
            this.hubUrlDid = hubUrlDid;
            this.keyPair = keyPair;
        }

        @Override
        public void register(DidResolver resolver) {

        }

        @Override
        public Result<DidDocument> resolve(String didKey) {
            try {
                var did = new ObjectMapper().readValue(hubUrlDid, DidDocument.class);
                ECKey key = (ECKey) keyPair.toPublicJWK();
                did.getVerificationMethod().add(VerificationMethod.Builder.create()
                        .type("JsonWebKey2020")
                        .id("test-key")
                        .publicKeyJwk(new EllipticCurvePublicKey(key.getCurve().getName(), key.getKeyType().toString(), key.getX().toString(), key.getY().toString()))
                        .build());
                return Result.success(did);
            } catch (JsonProcessingException e) {
                throw new AssertionError(e);
            }
        }
    }

}
