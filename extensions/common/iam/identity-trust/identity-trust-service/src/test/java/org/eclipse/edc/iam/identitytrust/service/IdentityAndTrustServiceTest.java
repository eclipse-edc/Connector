/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.service;


import org.eclipse.edc.identitytrust.CredentialServiceClient;
import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.VerifiablePresentationContainer;
import org.eclipse.edc.identitytrust.verifier.PresentationVerifier;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.eclipse.edc.identitytrust.TestFunctions.createCredentialBuilder;
import static org.eclipse.edc.identitytrust.TestFunctions.createJwt;
import static org.eclipse.edc.identitytrust.TestFunctions.createPresentationBuilder;
import static org.eclipse.edc.identitytrust.TestFunctions.createPresentationContainer;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IdentityAndTrustServiceTest {
    public static final String EXPECTED_OWN_DID = "did:web:test";
    private final SecureTokenService mockedSts = mock();
    private final PresentationVerifier mockedVerifier = mock();
    private final CredentialServiceClient mockedClient = mock();
    private final IdentityAndTrustService service = new IdentityAndTrustService(mockedSts, EXPECTED_OWN_DID, mockedVerifier, mockedClient, mock());


    @Nested
    class VerifyObtainToken {
        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "org.eclipse.edc:TestCredential:modify", "org.eclipse.edc:TestCredential:", "org.eclipse.edc:TestCredential: ", "org.eclipse.edc:TestCredential:write*", ":TestCredential:read",
                "org.eclipse.edc:fooCredential:+" })
        void obtainClientCredentials_invalidScopeString(String scope) {
            var tp = TokenParameters.Builder.newInstance()
                    .scope(scope)
                    .audience("test-audience")
                    .build();
            assertThat(service.obtainClientCredentials(tp))
                    .isNotNull()
                    .isFailed()
                    .detail().contains("Scope string invalid");
        }

        @ParameterizedTest(name = "Scope: {0}")
        @ValueSource(strings = { "org.eclipse.edc:TestCredential:modify", "org.eclipse.edc:TestCredential:", "org.eclipse.edc:TestCredential: ", "org.eclipse.edc:TestCredential:write*", ":TestCredential:read",
                "org.eclipse.edc:fooCredential:+" })
        @NullSource
        @EmptySource
        void obtainClientCredentials_validScopeString(String scope) {
            var tp = TokenParameters.Builder.newInstance()
                    .scope(scope)
                    .audience("test-audience")
                    .build();
            assertThat(service.obtainClientCredentials(tp))
                    .isNotNull()
                    .isFailed()
                    .detail().contains("Scope string invalid");
        }


        @Test
        void obtainClientCredentials_stsFails() {
            var scope = "org.eclipse.edc.vp.type:TestCredential:read";
            var tp = TokenParameters.Builder.newInstance()
                    .scope(scope)
                    .audience("test-audience")
                    .build();
            when(mockedSts.createToken(any(), any())).thenReturn(success(createJwt()));
            assertThat(service.obtainClientCredentials(tp)).isSucceeded();
            verify(mockedSts).createToken(argThat(m -> m.get("iss").equals(EXPECTED_OWN_DID) &&
                    m.get("sub").equals(EXPECTED_OWN_DID) &&
                    m.get("aud").equals(tp.getAudience())), eq(scope));
        }
    }

    @Nested
    class PresentationValidation {

        @Test
        void presentationRequestFails() {
            when(mockedClient.requestPresentation(any(), any(), any())).thenReturn(failure("test-failure"));
            var token = createJwt();
            var result = service.verifyJwtToken(token, "test-audience");
            assertThat(result).isFailed().detail().isEqualTo("test-failure");
            verifyNoInteractions(mockedVerifier);
            verify(mockedClient).requestPresentation(any(), any(), any());

        }

        @Test
        void cryptographicError() {
            when(mockedVerifier.verifyPresentation(anyString(), any(CredentialFormat.class))).thenReturn(Result.failure("Cryptographic error"));
            when(mockedClient.requestPresentation(any(), any(), any())).thenReturn(success(createPresentationContainer()));
            var token = createJwt();
            var result = service.verifyJwtToken(token, "test-audience");
            assertThat(result).isFailed().detail().isEqualTo("Cryptographic error");
        }

        @Test
        void oneInvalidSubjectId() {
            var presentation = createPresentationBuilder()
                    .type("VerifiablePresentation")
                    .credentials(List.of(createCredentialBuilder()
                            .credentialSubject(List.of(CredentialSubject.Builder.newInstance()
                                    .id("invalid-subject-id")
                                    .claim("some-claim", "some-val")
                                    .build()))
                            .build()))
                    .build();
            var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.JSON_LD, presentation);
            when(mockedVerifier.verifyPresentation(anyString(), any(CredentialFormat.class))).thenReturn(success());
            when(mockedClient.requestPresentation(any(), any(), any())).thenReturn(success(vpContainer));
            var consumerDid = "did:web:test-consumer";
            var token = createJwt(consumerDid, EXPECTED_OWN_DID);
            var result = service.verifyJwtToken(token, "test-audience");
            assertThat(result).isFailed().messages()
                    .hasSizeGreaterThanOrEqualTo(1)
                    .contains("Not all subject IDs match the expected subject ID %s".formatted(consumerDid));
        }

        @Disabled("Not yet implemented")
        @Test
        void credentialIsRevoked() {
            // not yet implemented
        }

        @Test
        void credentialHasInvalidIssuer_issuerIsUrl() {
            var consumerDid = "did:web:test-consumer";
            var presentation = createPresentationBuilder()
                    .type("VerifiablePresentation")
                    .credentials(List.of(createCredentialBuilder()
                            .issuer("invalid-issuer")
                            .build()))
                    .build();
            var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.JSON_LD, presentation);
            when(mockedVerifier.verifyPresentation(anyString(), any(CredentialFormat.class))).thenReturn(success());
            when(mockedClient.requestPresentation(any(), any(), any())).thenReturn(success(vpContainer));
            var token = createJwt(consumerDid, EXPECTED_OWN_DID);
            var result = service.verifyJwtToken(token, "test-audience");
            assertThat(result).isFailed().messages()
                    .hasSizeGreaterThanOrEqualTo(1)
                    .contains("Issuer 'invalid-issuer' is not in the list of allowed issuers");
        }

    }

}