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
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.KeyPairFactory;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.EllipticCurvePublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.document.VerificationMethod;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.iam.TokenParameters;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the {@link DecentralizedIdentityService} with a key algorithm. See {@link WithP256Test} for concrete impl.
 */

abstract class DecentralizedIdentityServiceTest {
    private static final String DID_DOCUMENT = getResourceFileContentAsString("dids.json");

    private JWK keyPair;
    private CredentialsVerifier credentialsVerifierMock;
    private DidResolverRegistry didResolverRegistryMock;
    private DecentralizedIdentityService identityService;

    @Test
    void generateAndVerifyJwtToken_valid() {
        when(credentialsVerifierMock.getVerifiedCredentials(any())).thenReturn(Result.success(Map.of("region", "eu")));
        when(didResolverRegistryMock.resolve(anyString())).thenReturn(Result.success(createDidDocument((ECKey) keyPair.toPublicJWK())));

        var result = identityService.obtainClientCredentials(defaultTokenParameters());
        assertTrue(result.succeeded());

        var verificationResult = identityService.verifyJwtToken(result.getContent(), "Bar");
        assertTrue(verificationResult.succeeded());
        assertEquals("eu", verificationResult.getContent().getClaims().get("region"));
    }

    @Test
    void generateAndVerifyJwtToken_wrongPublicKey() {
        var otherKeyPair = getKeyPair();
        when(credentialsVerifierMock.getVerifiedCredentials(any())).thenReturn(Result.success(Map.of("region", "eu")));
        when(didResolverRegistryMock.resolve(anyString())).thenReturn(Result.success(createDidDocument((ECKey) otherKeyPair.toPublicJWK())));

        var result = identityService.obtainClientCredentials(defaultTokenParameters());
        assertTrue(result.succeeded());

        var verificationResult = identityService.verifyJwtToken(result.getContent(), "Bar");
        assertTrue(verificationResult.failed());
        assertThat(verificationResult.getFailureMessages()).contains("Token could not be verified!");
    }

    @Test
    void generateAndVerifyJwtToken_wrongAudience() {
        when(didResolverRegistryMock.resolve(anyString())).thenReturn(Result.success(createDidDocument((ECKey) keyPair.toPublicJWK())));

        var result = identityService.obtainClientCredentials(defaultTokenParameters());

        var verificationResult = identityService.verifyJwtToken(result.getContent(), "Bar2");
        assertTrue(verificationResult.failed());
    }

    @Test
    void generateAndVerifyJwtToken_getVerifiedCredentialsFailed() {
        var errorMsg = UUID.randomUUID().toString();
        when(credentialsVerifierMock.getVerifiedCredentials(any())).thenReturn(Result.failure(errorMsg));
        when(didResolverRegistryMock.resolve(anyString())).thenReturn(Result.success(createDidDocument((ECKey) keyPair.toPublicJWK())));

        var result = identityService.obtainClientCredentials(defaultTokenParameters());
        assertTrue(result.succeeded());

        var verificationResult = identityService.verifyJwtToken(result.getContent(), "Bar");
        assertTrue(verificationResult.failed());
        assertThat(verificationResult.getFailureDetail()).contains(errorMsg);
    }

    private static TokenParameters defaultTokenParameters() {
        return TokenParameters.Builder.newInstance()
                .scope("Foo")
                .audience("Bar")
                .build();
    }

    private static DidDocument createDidDocument(ECKey publicKey) {
        try {
            var did = new ObjectMapper().readValue(DID_DOCUMENT, DidDocument.class);
            did.getVerificationMethod().add(VerificationMethod.Builder.create()
                    .type("JsonWebKey2020")
                    .id("test-key")
                    .publicKeyJwk(new EllipticCurvePublicKey(publicKey.getCurve().getName(), publicKey.getKeyType().toString(), publicKey.getX().toString(), publicKey.getY().toString()))
                    .build());
            return did;
        } catch (JsonProcessingException e) {
            throw new AssertionError(e);
        }
    }

    @BeforeEach
    void setUp() {
        keyPair = getKeyPair();
        var privateKey = new EcPrivateKeyWrapper(keyPair.toECKey());
        didResolverRegistryMock = mock(DidResolverRegistry.class);
        credentialsVerifierMock = mock(CredentialsVerifier.class);
        var didUrl = "random.did.url";
        identityService = new DecentralizedIdentityService(didResolverRegistryMock, credentialsVerifierMock, new ConsoleMonitor(), privateKey, didUrl, Clock.systemUTC());
    }

    @NotNull
    protected abstract JWK getKeyPair();

    public static class WithP256Test extends DecentralizedIdentityServiceTest {
        @Override
        protected @NotNull ECKey getKeyPair() {
            return KeyPairFactory.generateKeyPairP256();
        }

    }
}
