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

package org.eclipse.edc.iam.did.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.edc.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.edc.spi.agent.ParticipantAgentService.DEFAULT_IDENTITY_CLAIM_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the {@link DecentralizedIdentityService} with a key algorithm.
 */
abstract class BaseDecentralizedIdentityServiceTest {

    private static final String DID_URL = "random.did.url";
    private static final String DID_DOCUMENT = getResourceFileContentAsString("dids.json");

    private final CredentialsVerifier credentialsVerifierMock = mock(CredentialsVerifier.class);
    private final DidResolverRegistry didResolverRegistryMock = mock(DidResolverRegistry.class);
    private final JWK keyPair;

    private DecentralizedIdentityService identityService;

    protected BaseDecentralizedIdentityServiceTest(JWK keyPair) {
        this.keyPair = keyPair;
    }

    @BeforeEach
    void setUp() {
        identityService = new DecentralizedIdentityService(didResolverRegistryMock, credentialsVerifierMock, new ConsoleMonitor(), privateKeyWrapper(keyPair), DID_URL, Clock.systemUTC());
    }

    @Test
    void generateAndVerifyJwtToken_valid() {
        when(credentialsVerifierMock.getVerifiedCredentials(any())).thenReturn(Result.success(Map.of("region", "eu")));
        when(didResolverRegistryMock.resolve(anyString())).thenReturn(Result.success(createDidDocument(keyPair)));

        var result = identityService.obtainClientCredentials(defaultTokenParameters());
        assertThat(result.succeeded()).isTrue();

        var verificationResult = identityService.verifyJwtToken(result.getContent(), "Bar");
        assertThat(verificationResult.succeeded()).isTrue();

        var claimToken = verificationResult.getContent();
        assertThat(claimToken.getStringClaim("region")).isEqualTo("eu");
        assertThat(claimToken.getStringClaim(DEFAULT_IDENTITY_CLAIM_KEY)).isEqualTo(DID_URL);
    }

    @Test
    void generateAndVerifyJwtToken_wrongPublicKey() {
        var otherKeyPair = generateKeyPair();
        when(credentialsVerifierMock.getVerifiedCredentials(any())).thenReturn(Result.success(Map.of("region", "eu")));
        when(didResolverRegistryMock.resolve(anyString())).thenReturn(Result.success(createDidDocument(otherKeyPair)));

        var result = identityService.obtainClientCredentials(defaultTokenParameters());
        assertThat(result.succeeded()).isTrue();

        var verificationResult = identityService.verifyJwtToken(result.getContent(), "Bar");
        assertThat(verificationResult.failed()).isTrue();
        assertThat(verificationResult.getFailureMessages()).contains("Token could not be verified!");
    }

    @Test
    void generateAndVerifyJwtToken_wrongAudience() {
        when(didResolverRegistryMock.resolve(anyString())).thenReturn(Result.success(createDidDocument(keyPair)));

        var result = identityService.obtainClientCredentials(defaultTokenParameters());

        var verificationResult = identityService.verifyJwtToken(result.getContent(), "Bar2");
        assertThat(verificationResult.failed()).isTrue();
    }

    @Test
    void generateAndVerifyJwtToken_getVerifiedCredentialsFailed() {
        var errorMsg = UUID.randomUUID().toString();
        when(credentialsVerifierMock.getVerifiedCredentials(any())).thenReturn(Result.failure(errorMsg));
        when(didResolverRegistryMock.resolve(anyString())).thenReturn(Result.success(createDidDocument(keyPair)));

        var result = identityService.obtainClientCredentials(defaultTokenParameters());
        assertThat(result.succeeded()).isTrue();

        var verificationResult = identityService.verifyJwtToken(result.getContent(), "Bar");
        assertThat(verificationResult.failed()).isTrue();
        assertThat(verificationResult.getFailureDetail()).contains(errorMsg);
    }

    private static TokenParameters defaultTokenParameters() {
        return TokenParameters.Builder.newInstance()
                .scope("Foo")
                .audience("Bar")
                .build();
    }

    private DidDocument createDidDocument(JWK keyPair) {
        try {
            var did = new ObjectMapper().readValue(DID_DOCUMENT, DidDocument.class);
            var verificationMethod = VerificationMethod.Builder.create()
                    .type("JsonWebKey2020")
                    .id("test-key")
                    .publicKeyJwk(keyPair.toPublicJWK().toJSONObject())
                    .build();
            did.getVerificationMethod().add(verificationMethod);
            return did;
        } catch (JsonProcessingException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    protected abstract JWK generateKeyPair();

    @NotNull
    protected abstract PrivateKeyWrapper privateKeyWrapper(JWK keyPair);
}
