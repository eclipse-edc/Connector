/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.verifiablecredentials;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.protocol.spi.TrustedIssuer;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createCredentialBuilder;
import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createPresentationBuilder;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class VerifiableCredentialValidationServiceImplTest {
    public static final String CONSUMER_DID = "did:web:consumer";
    public static final String EXPECTED_AUDIENCE = "did:web:test";
    private final PresentationVerifier verifier = mock();
    private final VerifiableCredentialValidationServiceImpl validationService = new VerifiableCredentialValidationServiceImpl(verifier, emptyList());

    @Test
    void cryptographicError() {
        when(verifier.verifyPresentation(any(), any())).thenReturn(Result.failure("Cryptographic error"));
        var presentations = List.of(trustedPresentation());
        var result = validationService.validate(presentations, EXPECTED_AUDIENCE, createProfile());
        assertThat(result).isFailed().detail().isEqualTo("Cryptographic error");
    }

    @Test
    void verify_untrustedIssuer_shouldNotVerifyPresentation() {
        var presentation = createPresentationBuilder()
                .holder(CONSUMER_DID)
                .type("VerifiablePresentation")
                .credentials(List.of(createCredentialBuilder()
                        .issuer(new Issuer("did:web:untrusted", Map.of()))
                        .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                .id(CONSUMER_DID)
                                .claim("some-claim", "some-val")
                                .build()))
                        .build()))
                .build();
        var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);

        var result = validationService.validate(List.of(vpContainer), EXPECTED_AUDIENCE, createProfile());

        assertThat(result).isFailed().detail().contains("not supported for issuer 'did:web:untrusted'");
        verifyNoInteractions(verifier);
    }

    @Test
    void verify_invalidSubjectId_shouldNotVerifyPresentation() {
        var presentation = createPresentationBuilder()
                .holder(CONSUMER_DID)
                .type("VerifiablePresentation")
                .credentials(List.of(createCredentialBuilder()
                        .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                .id("invalid-subject-id")
                                .claim("some-claim", "some-val")
                                .build()))
                        .build()))
                .build();
        var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);

        var result = validationService.validate(List.of(vpContainer), EXPECTED_AUDIENCE, createProfile());

        assertThat(result).isFailed().detail().contains("Not all credential subject IDs match the expected subject ID");
        verifyNoInteractions(verifier);
    }

    @Test
    void verify_cryptographicError_shouldNotApplyBaseRules() {
        CredentialValidationRule baseRule = mock();
        var service = new VerifiableCredentialValidationServiceImpl(verifier, List.of(baseRule));
        when(verifier.verifyPresentation(any(), any())).thenReturn(Result.failure("Cryptographic error"));

        var result = service.validate(List.of(trustedPresentation()), EXPECTED_AUDIENCE, createProfile());

        assertThat(result).isFailed().detail().isEqualTo("Cryptographic error");
        verifyNoInteractions(baseRule);
    }

    @Test
    void oneInvalidSubjectId() {
        var presentation = createPresentationBuilder()
                .type("VerifiablePresentation")
                .holder(CONSUMER_DID)
                .credentials(List.of(createCredentialBuilder()
                        .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                .id("invalid-subject-id")
                                .claim("some-claim", "some-val")
                                .build()))
                        .build()))
                .build();
        var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
        when(verifier.verifyPresentation(any(), any())).thenReturn(success());
        var result = validationService.validate(List.of(vpContainer), EXPECTED_AUDIENCE, createProfile());
        assertThat(result).isFailed().messages()
                .hasSizeGreaterThanOrEqualTo(1)
                .contains("Not all credential subject IDs match the expected subject ID '%s'. Violating subject IDs: [invalid-subject-id]".formatted(CONSUMER_DID));
    }

    @Test
    void verify_singlePresentation_noCredential() {
        var presentation = createPresentationBuilder()
                .holder(CONSUMER_DID)
                .type("VerifiablePresentation")
                .credentials(List.of())
                .build();
        var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
        when(verifier.verifyPresentation(any(), any())).thenReturn(success());
        var result = validationService.validate(List.of(vpContainer), EXPECTED_AUDIENCE, createProfile());
        assertThat(result).isSucceeded();
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
        when(verifier.verifyPresentation(any(), any())).thenReturn(success());
        var result = validationService.validate(List.of(vpContainer), EXPECTED_AUDIENCE, createProfile());
        assertThat(result).isSucceeded();
    }

    @Test
    void verify_singlePresentation_noHolderProperty_shouldSucceed() {
        var presentation = createPresentationBuilder()
                .holder(null) // un-set holder property
                .type("VerifiablePresentation")
                .credentials(List.of(createCredentialBuilder()
                        .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                .id(CONSUMER_DID)
                                .claim("some-claim", "some-val")
                                .build()))
                        .build()))
                .build();
        var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
        when(verifier.verifyPresentation(any(), any())).thenReturn(success());
        var result = validationService.validate(List.of(vpContainer), EXPECTED_AUDIENCE, createProfile());
        assertThat(result).isSucceeded();
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
        when(verifier.verifyPresentation(any(), any())).thenReturn(success());
        var result = validationService.validate(List.of(vpContainer), EXPECTED_AUDIENCE, createProfile());
        assertThat(result).isSucceeded();
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
                .type("VerifiablePresentation")
                .holder(CONSUMER_DID)
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

        when(verifier.verifyPresentation(any(), any())).thenReturn(success());

        var result = validationService.validate(List.of(vpContainer1, vpContainer2), EXPECTED_AUDIENCE, createProfile());
        assertThat(result).isSucceeded();
    }

    @Test
    void verify_untrustedIssuer_shouldNotApplyBaseRules() {
        CredentialValidationRule baseRule = mock();
        var service = new VerifiableCredentialValidationServiceImpl(verifier, List.of(baseRule));
        var presentation = createPresentationBuilder()
                .holder(CONSUMER_DID)
                .type("VerifiablePresentation")
                .credentials(List.of(createCredentialBuilder()
                        .issuer(new Issuer("did:web:untrusted", Map.of()))
                        .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                .id(CONSUMER_DID)
                                .claim("some-claim", "some-val")
                                .build()))
                        .build()))
                .build();
        var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
        when(verifier.verifyPresentation(any(), any())).thenReturn(success());

        var result = service.validate(List.of(vpContainer), EXPECTED_AUDIENCE, createProfile());

        assertThat(result).isFailed().detail().contains("not supported for issuer 'did:web:untrusted'");
        verifyNoInteractions(baseRule);
    }

    @Test
    void verify_invalidSubjectId_shouldNotApplyBaseRules() {
        CredentialValidationRule baseRule = mock();
        var service = new VerifiableCredentialValidationServiceImpl(verifier, List.of(baseRule));
        var presentation = createPresentationBuilder()
                .holder(CONSUMER_DID)
                .type("VerifiablePresentation")
                .credentials(List.of(createCredentialBuilder()
                        .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                .id("invalid-subject-id")
                                .claim("some-claim", "some-val")
                                .build()))
                        .build()))
                .build();
        var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
        when(verifier.verifyPresentation(any(), any())).thenReturn(success());

        var result = service.validate(List.of(vpContainer), EXPECTED_AUDIENCE, createProfile());

        assertThat(result).isFailed().detail().contains("Not all credential subject IDs match the expected subject ID");
        verifyNoInteractions(baseRule);
    }

    @Test
    void verify_trustedIssuer_shouldApplyBaseRules() {
        CredentialValidationRule baseRule = mock();
        when(baseRule.apply(any())).thenReturn(Result.failure("base rule failed"));
        var service = new VerifiableCredentialValidationServiceImpl(verifier, List.of(baseRule));
        var credential = createCredentialBuilder()
                .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                        .id(CONSUMER_DID)
                        .claim("some-claim", "some-val")
                        .build()))
                .build();
        var presentation = createPresentationBuilder()
                .holder(CONSUMER_DID)
                .type("VerifiablePresentation")
                .credentials(List.of(credential))
                .build();
        var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
        when(verifier.verifyPresentation(any(), any())).thenReturn(success());

        var result = service.validate(List.of(vpContainer), EXPECTED_AUDIENCE, createProfile());

        assertThat(result).isFailed().detail().isEqualTo("base rule failed");
        verify(baseRule).apply(credential);
    }

    @Test
    void verify_shouldBeRetryable_whenBaseRuleCouldNotBeChecked() {
        CredentialValidationRule revocationRule = mock();
        when(revocationRule.apply(any())).thenReturn(Result.retryableFailure("status list not reachable"));
        CredentialValidationRule otherRule = mock();
        when(otherRule.apply(any())).thenReturn(success());
        var service = new VerifiableCredentialValidationServiceImpl(verifier, List.of(revocationRule, otherRule));
        when(verifier.verifyPresentation(any(), any())).thenReturn(success());

        var result = service.validate(List.of(trustedPresentation()), EXPECTED_AUDIENCE, createProfile());

        assertThat(result).isFailed().detail().isEqualTo("status list not reachable");
        assertThat(result.isRetryable()).isTrue();
    }

    @Test
    void verify_shouldNotBeRetryable_whenAnotherBaseRuleFailed() {
        CredentialValidationRule revocationRule = mock();
        when(revocationRule.apply(any())).thenReturn(Result.retryableFailure("status list not reachable"));
        CredentialValidationRule expiryRule = mock();
        when(expiryRule.apply(any())).thenReturn(Result.failure("credential expired"));
        var service = new VerifiableCredentialValidationServiceImpl(verifier, List.of(revocationRule, expiryRule));
        when(verifier.verifyPresentation(any(), any())).thenReturn(success());

        var result = service.validate(List.of(trustedPresentation()), EXPECTED_AUDIENCE, createProfile());

        assertThat(result).isFailed();
        assertThat(result.isRetryable()).isFalse();
    }

    private VerifiablePresentationContainer trustedPresentation() {
        var credential = createCredentialBuilder()
                .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                        .id(CONSUMER_DID)
                        .claim("some-claim", "some-val")
                        .build()))
                .build();
        var presentation = createPresentationBuilder()
                .holder(CONSUMER_DID)
                .type("VerifiablePresentation")
                .credentials(List.of(credential))
                .build();
        return new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
    }

    private DataspaceProfileContext createProfile() {
        var trustedIssuer = TrustedIssuer.Builder.newInstance().id("http://test.issuer").supportedTypes(List.of("*")).build();
        return new DataspaceProfileContext("any", new ProtocolVersion("any", "any", "any"), mock(), mock(),
                new JsonLdNamespace("https://example.org/ns/"), List.of(), List.of(trustedIssuer));
    }
}