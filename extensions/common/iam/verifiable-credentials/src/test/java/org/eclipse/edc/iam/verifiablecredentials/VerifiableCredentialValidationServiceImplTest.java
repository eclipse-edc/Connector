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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.TRUSTED_ISSUER;
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
    private final PresentationVerifier verifierMock = mock();
    private final TrustedIssuerRegistry trustedIssuerRegistryMock = mock();
    private final RevocationServiceRegistry revocationServiceRegistry = mock();
    private final VerifiableCredentialValidationServiceImpl service = new VerifiableCredentialValidationServiceImpl(verifierMock, trustedIssuerRegistryMock, revocationServiceRegistry, Clock.systemUTC());

    @BeforeEach
    void setUp() {
        when(trustedIssuerRegistryMock.getSupportedTypes(TRUSTED_ISSUER)).thenReturn(Set.of(TrustedIssuerRegistry.WILDCARD));
        when(revocationServiceRegistry.checkValidity(any())).thenReturn(Result.success());
    }

    @Test
    void cryptographicError() {
        when(verifierMock.verifyPresentation(any())).thenReturn(Result.failure("Cryptographic error"));
        var presentations = List.of(createPresentationContainer());
        var result = service.validate(presentations);
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
        var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
        when(verifierMock.verifyPresentation(any())).thenReturn(success());
        var presentations = List.of(vpContainer);
        var result = service.validate(presentations);
        assertThat(result).isFailed().messages()
                .hasSizeGreaterThanOrEqualTo(1)
                .contains("Credential is not yet valid.");
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
        when(verifierMock.verifyPresentation(any())).thenReturn(success());
        var result = service.validate(List.of(vpContainer));
        assertThat(result).isFailed().messages()
                .hasSizeGreaterThanOrEqualTo(1)
                .contains("Not all credential subject IDs match the expected subject ID '%s'. Violating subject IDs: [invalid-subject-id]".formatted(CONSUMER_DID));
    }

    @Test
    void credentialHasInvalidIssuer_issuerIsUrl() {
        var presentation = createPresentationBuilder()
                .type("VerifiablePresentation")
                .credentials(List.of(createCredentialBuilder()
                        .issuer(new Issuer("invalid-issuer", Map.of()))
                        .build()))
                .build();

        var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
        when(verifierMock.verifyPresentation(any())).thenReturn(success());
        var result = service.validate(List.of(vpContainer));
        assertThat(result).isFailed().messages()
                .hasSizeGreaterThanOrEqualTo(1)
                .contains("Credential types '[test-type]' are not supported for issuer 'invalid-issuer'");
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
        when(verifierMock.verifyPresentation(any())).thenReturn(success());
        var result = service.validate(List.of(vpContainer));
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
        when(verifierMock.verifyPresentation(any())).thenReturn(success());
        var result = service.validate(List.of(vpContainer));
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

        when(verifierMock.verifyPresentation(any())).thenReturn(success());

        var result = service.validate(List.of(vpContainer1, vpContainer2));
        assertThat(result).isSucceeded();
    }

    @Test
    void verify_revocationCheckFails() {
        var presentation = createPresentationBuilder()
                .holder(CONSUMER_DID)
                .type("VerifiablePresentation")
                .credentials(List.of(createCredentialBuilder()
                        .credentialSubjects(List.of(CredentialSubject.Builder.newInstance()
                                .id(CONSUMER_DID)
                                .claim("some-claim", "some-val")
                                .build()))
                        .credentialStatus(new CredentialStatus("test-cred-status", "StatusList2021", Map.of()))
                        .build()))
                .build();
        var vpContainer = new VerifiablePresentationContainer("test-vp", CredentialFormat.VC1_0_LD, presentation);
        when(verifierMock.verifyPresentation(any())).thenReturn(success());
        when(revocationServiceRegistry.checkValidity(any())).thenReturn(Result.failure("invalid"));

        var result = service.validate(List.of(vpContainer));
        assertThat(result).isFailed()
                .detail().isEqualTo("invalid");
    }

}