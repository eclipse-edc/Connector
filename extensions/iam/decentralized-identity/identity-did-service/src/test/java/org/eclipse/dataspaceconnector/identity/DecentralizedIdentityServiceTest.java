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

package org.eclipse.dataspaceconnector.identity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.credentials.VerifiableCredentialFactory;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.KeyPairFactory;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.EllipticCurvePublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.document.VerificationMethod;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Test the {@link DecentralizedIdentityService} with different key algorithms.
 * See {@link WithP256} and {@link WithSecp256k1} for concrete impls.
 */

abstract class DecentralizedIdentityServiceTest {
    private final Instant now = Instant.now();
    private final Clock clock = Clock.fixed(now, UTC);
    private DecentralizedIdentityService identityService;
    private PrivateKeyWrapper privateKey;
    private PublicKeyWrapper publicKey;

    @Test
    void verifyResolveHubUrl() throws IOException {
        var didJson = Thread.currentThread().getContextClassLoader().getResourceAsStream("dids.json");
        var hubUrlDid = new String(didJson.readAllBytes(), StandardCharsets.UTF_8);
        var url = identityService.getHubUrl(new ObjectMapper().readValue(hubUrlDid, DidDocument.class));
        assertEquals("https://myhub.com", url);
    }

    @Test
    void verifyObtainClientCredentials() throws Exception {
        var result = identityService.obtainClientCredentials("Foo");

        assertTrue(result.succeeded());

        var jwt = SignedJWT.parse(result.getContent().getToken());
        var verifier = publicKey.verifier();
        assertTrue(jwt.verify(verifier));
    }

    @Test
    void verifyJwtToken() throws Exception {
        var signer = privateKey.signer();

        var claimsSet = new JWTClaimsSet.Builder()
                .subject("foo")
                .issuer("did:ion:123abc")
                .expirationTime(Date.from(now.plus(Duration.ofMinutes(10))))
                .build();

        var jwt = new SignedJWT(new JWSHeader.Builder(getHeaderAlgorithm()).keyID("primary").build(), claimsSet);
        jwt.sign(signer);

        var token = jwt.serialize();

        var result = identityService.verifyJwtToken(token);

        assertTrue(result.succeeded());
        assertEquals("eu", result.getContent().getClaims().get("region"));
    }

    @BeforeEach
    void setUp() throws Exception {
        var keyPair = getKeyPair();
        privateKey = getPrivateKey(keyPair.toECKey());
        publicKey = getPublicKey(keyPair.toPublicJWK().toECKey());

        var didJson = Thread.currentThread().getContextClassLoader().getResourceAsStream("dids.json");
        var hubUrlDid = new String(didJson.readAllBytes(), StandardCharsets.UTF_8);

        DidResolverRegistry didResolver = new TestResolverRegistry(hubUrlDid, keyPair);

        CredentialsVerifier verifier = (document, url) -> Result.success(Map.of("region", "eu"));
        Supplier<SignedJWT> signedJwtSupplier = () -> VerifiableCredentialFactory.create(privateKey, Map.of("region", "us"), "test-issuer", clock);
        identityService = new DecentralizedIdentityService(signedJwtSupplier, didResolver, verifier, mock(Monitor.class), Clock.systemUTC());

    }

    @NotNull
    protected abstract JWK getKeyPair();

    @NotNull
    protected abstract JWSAlgorithm getHeaderAlgorithm();

    private PublicKeyWrapper getPublicKey(JWK publicKey) {

        return new EcPublicKeyWrapper((ECKey) publicKey);
    }

    private PrivateKeyWrapper getPrivateKey(JWK privateKey) {
        return new EcPrivateKeyWrapper((ECKey) privateKey);
    }

    public static class WithP256 extends DecentralizedIdentityServiceTest {
        @Override
        protected @NotNull JWK getKeyPair() {
            return KeyPairFactory.generateKeyPairP256();
        }

        @Override
        protected @NotNull JWSAlgorithm getHeaderAlgorithm() {
            return JWSAlgorithm.ES256;
        }
    }

    private static class TestResolverRegistry implements DidResolverRegistry {
        private String hubUrlDid;
        private JWK keyPair;

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
