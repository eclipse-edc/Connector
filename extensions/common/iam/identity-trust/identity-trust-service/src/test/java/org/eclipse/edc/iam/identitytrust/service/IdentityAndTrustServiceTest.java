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
 *       Cofinity-X - updates for VCDM 2.0
 *
 */

package org.eclipse.edc.iam.identitytrust.service;


import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.edc.iam.identitytrust.spi.CredentialServiceClient;
import org.eclipse.edc.iam.identitytrust.spi.CredentialServiceUrlResolver;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.iam.identitytrust.spi.TestFunctions;
import org.eclipse.edc.iam.identitytrust.spi.validation.TokenValidationAction;
import org.eclipse.edc.iam.verifiablecredentials.spi.VerifiableCredentialValidationService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createCredentialBuilder;
import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createPresentationBuilder;
import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createPresentationContainer;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SCOPE;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IdentityAndTrustServiceTest {
    public static final String EXPECTED_OWN_DID = "did:web:test";

    public static final String CONSUMER_DID = "did:web:consumer";
    private static final String PARTICIPANT_CONTEXT_ID = "participantContextId";
    private final SecureTokenService mockedSts = mock();
    private final CredentialServiceClient mockedClient = mock();
    private final CredentialServiceUrlResolver credentialServiceUrlResolverMock = mock();
    private final TokenValidationAction actionMock = mock();
    private final VerifiableCredentialValidationService credentialValidationServiceMock = mock();
    private final IdentityAndTrustService service = new IdentityAndTrustService(mockedSts, EXPECTED_OWN_DID, mockedClient,
            actionMock, credentialServiceUrlResolverMock, vcs -> Result.success(ClaimToken.Builder.newInstance().claim("vc", vcs).build()),
            credentialValidationServiceMock
    );

    @BeforeEach
    void setup() {
        when(credentialServiceUrlResolverMock.resolve(any())).thenReturn(success("foobar"));
        var jwt = TestFunctions.createToken(new JWTClaimsSet.Builder().claim("scope", "foo-scope").build());

        when(actionMock.apply(any())).thenReturn(success(ClaimToken.Builder.newInstance()
                .claim("iss", CONSUMER_DID)
                .claim(PRESENTATION_TOKEN_CLAIM, jwt.getToken()).build()));

        when(mockedSts.createToken(any(), any())).thenReturn(success(TokenRepresentation.Builder.newInstance().build()));

        when(credentialValidationServiceMock.validate(anyList(), anyCollection()))
                .thenReturn(Result.success());
    }

    private VerificationContext verificationContext() {
        return VerificationContext.Builder.newInstance()
                .policy(Policy.Builder.newInstance().build())
                .scopes(List.of("org.eclipse.edc.vc.type:test-type:read"))
                .build();
    }

    @Nested
    class ObtainClientCredentials {
        @ParameterizedTest(name = "Invalid Scope: {0}")
        @ValueSource(strings = {"org.eclipse.edc:TestCredential:modify", "org.eclipse.edc:TestCredential:", "org.eclipse.edc:TestCredential: ", "org.eclipse.edc:TestCredential:write*", ":TestCredential:read",
                "org.eclipse.edc:fooCredential:+"})
        @EmptySource
        @NullSource
        void obtainClientCredentials_invalidScopeString(String scope) {
            var tp = TokenParameters.Builder.newInstance()
                    .claims(SCOPE, scope)
                    .claims(AUDIENCE, "test-audience")
                    .build();
            assertThat(service.obtainClientCredentials(PARTICIPANT_CONTEXT_ID, tp))
                    .isNotNull()
                    .isFailed()
                    .detail().contains("Scope string invalid");
        }

        @ParameterizedTest(name = "Scope: {0}")
        @ValueSource(strings = {"org.eclipse.edc:TestCredential:read", "org.eclipse.edc:TestCredential:*", "org.eclipse.edc:TestCredential:write"})
        void obtainClientCredentials_validScopeString(String scope) {
            var tp = TokenParameters.Builder.newInstance()
                    .claims(SCOPE, scope)
                    .claims(AUDIENCE, "test-audience")
                    .build();
            var result = service.obtainClientCredentials(PARTICIPANT_CONTEXT_ID, tp);
            assertThat(result)
                    .isNotNull()
                    .isSucceeded();

            assertThat(result.getContent().getToken()).startsWith("Bearer ");
        }


        @Test
        void obtainClientCredentials_stsFails() {
            var scope = "org.eclipse.edc.vp.type:TestCredential:read";
            var tp = TokenParameters.Builder.newInstance()
                    .claims(SCOPE, scope)
                    .claims(AUDIENCE, "test-audience")
                    .build();
            when(mockedSts.createToken(any(), any())).thenReturn(success(TestFunctions.createToken()));
            assertThat(service.obtainClientCredentials(PARTICIPANT_CONTEXT_ID, tp)).isSucceeded();
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
            when(mockedClient.requestPresentation(any(), any(), isA(List.class))).thenReturn(failure("test-failure"));
            var token = TestFunctions.createToken();
            var result = service.verifyJwtToken(PARTICIPANT_CONTEXT_ID, token, verificationContext());
            assertThat(result).isFailed().detail().isEqualTo("test-failure");
            verifyNoInteractions(credentialValidationServiceMock);
            verify(mockedClient).requestPresentation(any(), any(), isA(List.class));

        }

        @Test
        void credentialValidationServiceFails() {
            when(credentialValidationServiceMock.validate(anyList(), anyCollection()))
                    .thenReturn(Result.failure("test error"));
            when(mockedClient.requestPresentation(any(), any(), isA(List.class))).thenReturn(success(List.of(createPresentationContainer())));
            var token = TestFunctions.createToken();
            var result = service.verifyJwtToken(PARTICIPANT_CONTEXT_ID, token, verificationContext());
            assertThat(result).isFailed().detail().isEqualTo("test error");

        }

        @Test
        void jwtTokenNotValid() {
            when(actionMock.apply(any())).thenReturn(failure("test failure"));

            var token = TestFunctions.createToken();
            assertThat(service.verifyJwtToken(PARTICIPANT_CONTEXT_ID, token, verificationContext()))
                    .isFailed()
                    .messages().hasSize(1)
                    .containsExactly("test failure");
        }

        @Test
        void jwtTokenNotVerified() {
            when(actionMock.apply(any())).thenReturn(failure("test-failure"));
            var token = TestFunctions.createToken();
            assertThat(service.verifyJwtToken(PARTICIPANT_CONTEXT_ID, token, verificationContext()))
                    .isFailed()
                    .messages().hasSize(1)
                    .containsExactly("test-failure");
        }

        @Test
        void cannotResolveCredentialServiceUrl() {
            when(credentialServiceUrlResolverMock.resolve(any())).thenReturn(Result.failure("test-failure"));
            assertThat(service.verifyJwtToken(PARTICIPANT_CONTEXT_ID, TestFunctions.createToken(), verificationContext()))
                    .isFailed()
                    .detail()
                    .isEqualTo("test-failure");

            verifyNoInteractions(mockedClient);
        }

        @Test
        void verify_failsWithWrongHolder() {
            var presentation = createPresentationBuilder()
                    .holder("did:web:wrong")
                    .type("VerifiablePresentation")
                    .credentials(List.of(createCredentialBuilder()
                            .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                    .id(CONSUMER_DID)
                                    .claim("some-claim", "some-val")
                                    .build()))
                            .build()))
                    .build();
            var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
            when(mockedClient.requestPresentation(any(), any(), isA(List.class))).thenReturn(success(List.of(vpContainer)));
            when(credentialValidationServiceMock.validate(anyList(), anyCollection())).thenReturn(success());
            var token = TestFunctions.createToken(CONSUMER_DID, EXPECTED_OWN_DID);
            var result = service.verifyJwtToken(PARTICIPANT_CONTEXT_ID, token, verificationContext());
            assertThat(result).isFailed()
                    .detail()
                    .isEqualTo("Returned presentations contains invalid issuer. Expected did:web:consumer found [did:web:wrong]");
        }

        @Test
        void verify_singlePresentation_singleCredential() {
            var presentation = createPresentationBuilder()
                    .holder(CONSUMER_DID)
                    .type("VerifiablePresentation")
                    .credentials(List.of(createCredentialBuilder()
                            .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                    .id(CONSUMER_DID)
                                    .claim("some-claim", "some-val")
                                    .build()))
                            .build()))
                    .build();
            var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
            when(mockedClient.requestPresentation(any(), any(), isA(List.class))).thenReturn(success(List.of(vpContainer)));
            when(credentialValidationServiceMock.validate(anyList(), anyCollection())).thenReturn(success());
            var token = TestFunctions.createToken(CONSUMER_DID, EXPECTED_OWN_DID);
            var result = service.verifyJwtToken(PARTICIPANT_CONTEXT_ID, token, verificationContext());
            assertThat(result).isSucceeded()
                    .satisfies(ct -> {
                        var vc = (List<VerifiableCredential>) ct.getListClaim("vc");
                        assertThat(vc).hasSize(1);
                        assertThat(vc.get(0).getCredentialSubject().get(0).getClaims()).containsEntry("some-claim", "some-val");
                    });
        }

        @Test
        void verify_singlePresentation_multipleCredentials() {
            var presentation = createPresentationBuilder()
                    .holder(CONSUMER_DID)
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
            var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
            when(mockedClient.requestPresentation(any(), any(), isA(List.class))).thenReturn(success(List.of(vpContainer)));
            when(credentialValidationServiceMock.validate(anyList(), anyCollection())).thenReturn(success());
            var token = TestFunctions.createToken(CONSUMER_DID, EXPECTED_OWN_DID);
            var result = service.verifyJwtToken(PARTICIPANT_CONTEXT_ID, token, verificationContext());
            assertThat(result).isSucceeded()
                    .satisfies(ct -> {
                        var credentials = (List<VerifiableCredential>) ct.getClaims().get("vc");
                        assertThat(credentials).hasSize(2);
                        assertThat(credentials.get(0).getCredentialSubject().get(0).getClaims()).containsEntry("some-claim", "some-val");
                        assertThat(credentials.get(1).getCredentialSubject().get(0).getClaims()).containsEntry("some-other-claim", "some-other-val");
                    });
        }

        @Test
        void verify_multiplePresentations_multipleCredentialsEach() {
            var presentation1 = createPresentationBuilder()
                    .holder(CONSUMER_DID)
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
            var vpContainer1 = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation1);

            var presentation2 = createPresentationBuilder()
                    .holder(CONSUMER_DID)
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
            var vpContainer2 = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation2);

            when(mockedClient.requestPresentation(any(), any(), isA(List.class))).thenReturn(success(List.of(vpContainer1, vpContainer2)));
            when(credentialValidationServiceMock.validate(anyList(), anyCollection())).thenReturn(success());

            var token = TestFunctions.createToken(CONSUMER_DID, EXPECTED_OWN_DID);
            var result = service.verifyJwtToken(PARTICIPANT_CONTEXT_ID, token, verificationContext());
            assertThat(result).isSucceeded()
                    .satisfies(ct -> {
                        var credentials = (List<VerifiableCredential>) ct.getListClaim("vc");
                        assertThat(credentials).hasSize(4);
                        assertThat(credentials).anySatisfy(vc -> assertThat(vc.getCredentialSubject().get(0).getClaims()).containsEntry("some-claim", "some-val"));
                        assertThat(credentials).anySatisfy(vc -> assertThat(vc.getCredentialSubject().get(0).getClaims()).containsEntry("some-other-claim", "some-other-val"));
                        assertThat(credentials).anySatisfy(vc -> assertThat(vc.getCredentialSubject().get(0).getClaims()).containsEntry("some-claim-2", "some-val-2"));
                        assertThat(credentials).anySatisfy(vc -> assertThat(vc.getCredentialSubject().get(0).getClaims()).containsEntry("some-other-claim-2", "some-other-val-2"));
                    });
        }

        @Test
        void verify_requestedCredentialMissing_byNumber() {
            var presentation = createPresentationBuilder()
                    .holder(CONSUMER_DID)
                    .type("VerifiablePresentation")
                    .credentials(List.of(createCredentialBuilder()
                            .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                    .id(CONSUMER_DID)
                                    .claim("some-claim", "some-val")
                                    .build()))
                            .build()))
                    .build();
            var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
            when(mockedClient.requestPresentation(any(), any(), isA(List.class))).thenReturn(success(List.of(vpContainer)));
            when(credentialValidationServiceMock.validate(anyList(), anyCollection())).thenReturn(success());
            var token = TestFunctions.createToken(CONSUMER_DID, EXPECTED_OWN_DID);

            var context = VerificationContext.Builder.newInstance()
                    .policy(Policy.Builder.newInstance().build())
                    .scopes(List.of("org.eclipse.edc.vc.type:test-type:read", "org.eclipse.edc.vc.type:not-provided-type:read")) //should trigger a failure
                    .build();

            var result = service.verifyJwtToken(PARTICIPANT_CONTEXT_ID, token, context);

            assertThat(result).isFailed()
                    .detail()
                    .isEqualTo("Number of requested credentials does not match the number of returned credentials");
        }

        @SuppressWarnings("unchecked")
        @Test
        void verify_requestedCredentialMissing_byType() {
            var presentation = createPresentationBuilder()
                    .holder(CONSUMER_DID)
                    .type("VerifiablePresentation")
                    .credentials(List.of(createCredentialBuilder()
                            .type("org.eclipse.edc.vc.type:test-type:read")
                            .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                    .id(CONSUMER_DID)
                                    .claim("some-claim", "some-val")
                                    .build()))
                            .build()))
                    .build();
            var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
            when(mockedClient.requestPresentation(any(), any(), isA(List.class))).thenReturn(success(List.of(vpContainer)));
            when(credentialValidationServiceMock.validate(anyList(), anyCollection())).thenReturn(success());
            var token = TestFunctions.createToken(CONSUMER_DID, EXPECTED_OWN_DID);

            var context = VerificationContext.Builder.newInstance()
                    .policy(Policy.Builder.newInstance().build())
                    .scopes(List.of("org.eclipse.edc.vc.type:not-provided-type:read")) //should trigger a failure
                    .build();

            var result = service.verifyJwtToken(PARTICIPANT_CONTEXT_ID, token, context);

            assertThat(result).isFailed()
                    .detail()
                    .isEqualTo("Not all requested credentials are present in the presentation response");
        }
    }


}
