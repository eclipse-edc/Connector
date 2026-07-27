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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.protocol.spi.TrustedIssuer;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createCredentialBuilder;
import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createPresentationBuilder;
import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createPresentationContainer;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        var presentations = List.of(createPresentationContainer());
        var result = validationService.validate(presentations, EXPECTED_AUDIENCE, createProfile());
        assertThat(result).isFailed().detail().isEqualTo("Cryptographic error");
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

    private DataspaceProfileContext createProfile() {
        var trustedIssuer = TrustedIssuer.Builder.newInstance().id("http://test.issuer").supportedTypes(List.of("*")).build();
        return new DataspaceProfileContext("any", new ProtocolVersion("any", "any", "any"), mock(), mock(),
                new JsonLdNamespace("https://example.org/ns/"), List.of(), List.of(trustedIssuer));
    }
}