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


import com.nimbusds.jwt.JWTClaimsSet;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.iam.identitytrust.IdentityAndTrustService;
import org.eclipse.edc.identitytrust.CredentialServiceClient;
import org.eclipse.edc.identitytrust.CredentialServiceUrlResolver;
import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.identitytrust.TrustedIssuerRegistry;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.Issuer;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.model.VerifiablePresentationContainer;
import org.eclipse.edc.identitytrust.validation.TokenValidationAction;
import org.eclipse.edc.identitytrust.verification.PresentationVerifier;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.edc.identitytrust.SelfIssuedTokenConstants.PRESENTATION_ACCESS_TOKEN_CLAIM;
import static org.eclipse.edc.identitytrust.TestFunctions.TRUSTED_ISSUER;
import static org.eclipse.edc.identitytrust.TestFunctions.createCredentialBuilder;
import static org.eclipse.edc.identitytrust.TestFunctions.createJwt;
import static org.eclipse.edc.identitytrust.TestFunctions.createPresentationBuilder;
import static org.eclipse.edc.identitytrust.TestFunctions.createPresentationContainer;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SCOPE;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IdentityAndTrustServiceTest {
    public static final String EXPECTED_OWN_DID = "did:web:test";

    public static final String CONSUMER_DID = "did:web:consumer";
    private final SecureTokenService mockedSts = mock();
    private final PresentationVerifier mockedVerifier = mock();
    private final CredentialServiceClient mockedClient = mock();
    private final TrustedIssuerRegistry trustedIssuerRegistryMock = mock();
    private final CredentialServiceUrlResolver credentialServiceUrlResolverMock = mock();
    private final TokenValidationAction actionMock = mock();
    private final IdentityAndTrustService service = new IdentityAndTrustService(mockedSts, EXPECTED_OWN_DID, mockedVerifier, mockedClient,
            actionMock, trustedIssuerRegistryMock, Clock.systemUTC(), credentialServiceUrlResolverMock, vcs -> Result.success(ClaimToken.Builder.newInstance().claim("vc", vcs).build()));

    @BeforeEach
    void setup() {
        when(credentialServiceUrlResolverMock.resolve(any())).thenReturn(success("foobar"));
        var jwt = createJwt(new JWTClaimsSet.Builder().claim("scope", "foo-scope").build());

        when(actionMock.apply(any())).thenReturn(success(ClaimToken.Builder.newInstance()
                .claim("iss", CONSUMER_DID)
                .claim(PRESENTATION_ACCESS_TOKEN_CLAIM, jwt.getToken()).build()));

        when(mockedSts.createToken(any(), any())).thenReturn(success(TokenRepresentation.Builder.newInstance().build()));
    }

    private VerificationContext verificationContext() {
        return VerificationContext.Builder.newInstance()
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    @Nested
    class ObtainClientCredentials {
        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "org.eclipse.edc:TestCredential:modify", "org.eclipse.edc:TestCredential:", "org.eclipse.edc:TestCredential: ", "org.eclipse.edc:TestCredential:write*", ":TestCredential:read",
                "org.eclipse.edc:fooCredential:+" })
        void obtainClientCredentials_invalidScopeString(String scope) {
            var tp = TokenParameters.Builder.newInstance()
                    .claims(SCOPE, scope)
                    .claims(AUDIENCE, "test-audience")
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
                    .claims(SCOPE, scope)
                    .claims(AUDIENCE, "test-audience")
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
                    .claims(SCOPE, scope)
                    .claims(AUDIENCE, "test-audience")
                    .build();
            when(mockedSts.createToken(any(), any())).thenReturn(success(createJwt()));
            assertThat(service.obtainClientCredentials(tp)).isSucceeded();
            verify(mockedSts).createToken(argThat(m -> m.get("iss").equals(EXPECTED_OWN_DID) &&
                    m.get("sub").equals(EXPECTED_OWN_DID) &&
                    m.get("aud").equals(tp.getStringClaim(AUDIENCE))), eq(scope));
        }
    }


    @SuppressWarnings("unchecked")
    @Nested
    class VerifyJwtToken {

        @Test
        void presentationRequestFails() {
            when(mockedClient.requestPresentation(any(), any(), any())).thenReturn(failure("test-failure"));
            var token = createJwt();
            var result = service.verifyJwtToken(token, verificationContext());
            assertThat(result).isFailed().detail().isEqualTo("test-failure");
            verifyNoInteractions(mockedVerifier);
            verify(mockedClient).requestPresentation(any(), any(), any());

        }

        @Test
        void cryptographicError() {
            when(mockedVerifier.verifyPresentation(any())).thenReturn(Result.failure("Cryptographic error"));
            when(mockedClient.requestPresentation(any(), any(), any())).thenReturn(success(List.of(createPresentationContainer())));
            var token = createJwt();
            var result = service.verifyJwtToken(token, verificationContext());
            assertThat(result).isFailed().detail().isEqualTo("Cryptographic error");
        }

        @Test
        void notYetValid() {
            var presentation = createPresentationBuilder()
                    .type("VerifiablePresentation")
                    .credentials(List.of(createCredentialBuilder()
                            .issuanceDate(Instant.now().plus(10, ChronoUnit.DAYS))
                            .build()))
                    .build();
            var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.JSON_LD, presentation);
            when(mockedVerifier.verifyPresentation(any())).thenReturn(success());
            when(mockedClient.requestPresentation(any(), any(), any())).thenReturn(success(List.of(vpContainer)));
            var token = createJwt(CONSUMER_DID, EXPECTED_OWN_DID);
            var result = service.verifyJwtToken(token, verificationContext());
            assertThat(result).isFailed().messages()
                    .hasSizeGreaterThanOrEqualTo(1)
                    .contains("Credential is not yet valid.");
        }

        @Test
        void oneInvalidSubjectId() {
            var presentation = createPresentationBuilder()
                    .type("VerifiablePresentation")
                    .credentials(List.of(createCredentialBuilder()
                            .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                    .id("invalid-subject-id")
                                    .claim("some-claim", "some-val")
                                    .build()))
                            .build()))
                    .build();
            var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.JSON_LD, presentation);
            when(mockedVerifier.verifyPresentation(any())).thenReturn(success());
            when(mockedClient.requestPresentation(any(), any(), any())).thenReturn(success(List.of(vpContainer)));
            var token = createJwt(CONSUMER_DID, EXPECTED_OWN_DID);
            var result = service.verifyJwtToken(token, verificationContext());
            assertThat(result).isFailed().messages()
                    .hasSizeGreaterThanOrEqualTo(1)
                    .contains("Not all subject IDs match the expected subject ID %s".formatted(CONSUMER_DID));
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
                            .issuer(new Issuer("invalid-issuer", Map.of()))
                            .build()))
                    .build();
            var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.JSON_LD, presentation);
            when(mockedVerifier.verifyPresentation(any())).thenReturn(success());
            when(mockedClient.requestPresentation(any(), any(), any())).thenReturn(success(List.of(vpContainer)));
            var token = createJwt(consumerDid, EXPECTED_OWN_DID);
            var result = service.verifyJwtToken(token, verificationContext());
            assertThat(result).isFailed().messages()
                    .hasSizeGreaterThanOrEqualTo(1)
                    .contains("Issuer 'invalid-issuer' is not in the list of trusted issuers");
        }

        @Test
        void jwtTokenNotValid() {
            when(actionMock.apply(any())).thenReturn(failure("test failure"));

            var token = createJwt();
            assertThat(service.verifyJwtToken(token, verificationContext()))
                    .isFailed()
                    .messages().hasSize(1)
                    .containsExactly("test failure");
        }

        @Test
        void jwtTokenNotVerified() {
            when(actionMock.apply(any())).thenReturn(failure("test-failure"));
            var token = createJwt();
            assertThat(service.verifyJwtToken(token, verificationContext()))
                    .isFailed()
                    .messages().hasSize(1)
                    .containsExactly("test-failure");
        }

        @Test
        void cannotResolveCredentialServiceUrl() {
            when(credentialServiceUrlResolverMock.resolve(any())).thenReturn(Result.failure("test-failure"));
            assertThat(service.verifyJwtToken(createJwt(), verificationContext()))
                    .isFailed()
                    .detail()
                    .isEqualTo("test-failure");

            verifyNoInteractions(mockedClient);
        }

        @Test
        void verify_singlePresentation_singleCredential() {
            var presentation = createPresentationBuilder()
                    .type("VerifiablePresentation")
                    .credentials(List.of(createCredentialBuilder()
                            .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                    .id(CONSUMER_DID)
                                    .claim("some-claim", "some-val")
                                    .build()))
                            .build()))
                    .build();
            var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.JSON_LD, presentation);
            when(mockedVerifier.verifyPresentation(any())).thenReturn(success());
            when(mockedClient.requestPresentation(any(), any(), any())).thenReturn(success(List.of(vpContainer)));
            when(trustedIssuerRegistryMock.getTrustedIssuers()).thenReturn(Set.of(TRUSTED_ISSUER));
            var token = createJwt(CONSUMER_DID, EXPECTED_OWN_DID);
            var result = service.verifyJwtToken(token, verificationContext());
            assertThat(result).isSucceeded()
                    .satisfies(ct -> {
                        var vc = (List<VerifiableCredential>) ct.getListClaim("vc");
                        Assertions.assertThat(vc).hasSize(1);
                        Assertions.assertThat(vc.get(0).getCredentialSubject().get(0).getClaims()).containsEntry("some-claim", "some-val");
                    });
        }

        @Test
        void verify_singlePresentation_multipleCredentials() {
            var presentation = createPresentationBuilder()
                    .type("VerifiablePresentation")
                    .credentials(List.of(createCredentialBuilder()
                                    .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                            .id(CONSUMER_DID)
                                            .claim("some-claim", "some-val")
                                            .build()))
                                    .build(),
                            createCredentialBuilder()
                                    .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                            .id(CONSUMER_DID)
                                            .claim("some-other-claim", "some-other-val")
                                            .build()))
                                    .build()))
                    .build();
            var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.JSON_LD, presentation);
            when(mockedVerifier.verifyPresentation(any())).thenReturn(success());
            when(mockedClient.requestPresentation(any(), any(), any())).thenReturn(success(List.of(vpContainer)));
            when(trustedIssuerRegistryMock.getTrustedIssuers()).thenReturn(Set.of(TRUSTED_ISSUER));
            var token = createJwt(CONSUMER_DID, EXPECTED_OWN_DID);
            var result = service.verifyJwtToken(token, verificationContext());
            assertThat(result).isSucceeded()
                    .satisfies(ct -> {
                        var credentials = (List<VerifiableCredential>) ct.getClaims().get("vc");
                        Assertions.assertThat(credentials).hasSize(2);
                        Assertions.assertThat(credentials.get(0).getCredentialSubject().get(0).getClaims()).containsEntry("some-claim", "some-val");
                        Assertions.assertThat(credentials.get(1).getCredentialSubject().get(0).getClaims()).containsEntry("some-other-claim", "some-other-val");
                    });
        }

        @Test
        void verify_multiplePresentations_multipleCredentialsEach() {
            var presentation1 = createPresentationBuilder()
                    .type("VerifiablePresentation")
                    .credentials(List.of(createCredentialBuilder()
                                    .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                            .id(CONSUMER_DID)
                                            .claim("some-claim", "some-val")
                                            .build()))
                                    .build(),
                            createCredentialBuilder()
                                    .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                            .id(CONSUMER_DID)
                                            .claim("some-other-claim", "some-other-val")
                                            .build()))
                                    .build()))
                    .build();
            var vpContainer1 = new VerifiablePresentationContainer("test-vp", CredentialFormat.JSON_LD, presentation1);

            var presentation2 = createPresentationBuilder()
                    .type("VerifiablePresentation")
                    .credentials(List.of(createCredentialBuilder()
                                    .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                            .id(CONSUMER_DID)
                                            .claim("some-claim-2", "some-val-2")
                                            .build()))
                                    .build(),
                            createCredentialBuilder()
                                    .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                            .id(CONSUMER_DID)
                                            .claim("some-other-claim-2", "some-other-val-2")
                                            .build()))
                                    .build()))
                    .build();
            var vpContainer2 = new VerifiablePresentationContainer("test-vp", CredentialFormat.JSON_LD, presentation2);

            when(mockedVerifier.verifyPresentation(any())).thenReturn(success());
            when(mockedClient.requestPresentation(any(), any(), any())).thenReturn(success(List.of(vpContainer1, vpContainer2)));
            when(trustedIssuerRegistryMock.getTrustedIssuers()).thenReturn(Set.of(TRUSTED_ISSUER));


            var token = createJwt(CONSUMER_DID, EXPECTED_OWN_DID);
            var result = service.verifyJwtToken(token, verificationContext());
            assertThat(result).isSucceeded()
                    .satisfies(ct -> {
                        var credentials = (List<VerifiableCredential>) ct.getListClaim("vc");
                        Assertions.assertThat(credentials).hasSize(4);
                        Assertions.assertThat(credentials).anySatisfy(vc -> Assertions.assertThat(vc.getCredentialSubject().get(0).getClaims()).containsEntry("some-claim", "some-val"));
                        Assertions.assertThat(credentials).anySatisfy(vc -> Assertions.assertThat(vc.getCredentialSubject().get(0).getClaims()).containsEntry("some-other-claim", "some-other-val"));
                        Assertions.assertThat(credentials).anySatisfy(vc -> Assertions.assertThat(vc.getCredentialSubject().get(0).getClaims()).containsEntry("some-claim-2", "some-val-2"));
                        Assertions.assertThat(credentials).anySatisfy(vc -> Assertions.assertThat(vc.getCredentialSubject().get(0).getClaims()).containsEntry("some-other-claim-2", "some-other-val-2"));
                    });
        }
    }
}