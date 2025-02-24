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
 *
 */

package org.eclipse.edc.iam.verifiablecredentials;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.rules.HasValidIssuer;
import org.eclipse.edc.iam.verifiablecredentials.rules.HasValidSubjectIds;
import org.eclipse.edc.iam.verifiablecredentials.rules.HasValidSubjectSchema;
import org.eclipse.edc.iam.verifiablecredentials.rules.IsInValidityPeriod;
import org.eclipse.edc.iam.verifiablecredentials.rules.IsNotRevoked;
import org.eclipse.edc.iam.verifiablecredentials.spi.VerifiableCredentialValidationService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

public class VerifiableCredentialValidationServiceImpl implements VerifiableCredentialValidationService {
    private final PresentationVerifier presentationVerifier;
    private final TrustedIssuerRegistry trustedIssuerRegistry;
    private final RevocationServiceRegistry revocationServiceRegistry;
    private final Clock clock;
    private final ObjectMapper mapper;

    public VerifiableCredentialValidationServiceImpl(PresentationVerifier presentationVerifier, TrustedIssuerRegistry trustedIssuerRegistry, RevocationServiceRegistry revocationServiceRegistry, Clock clock, ObjectMapper mapper) {
        this.presentationVerifier = presentationVerifier;
        this.trustedIssuerRegistry = trustedIssuerRegistry;
        this.revocationServiceRegistry = revocationServiceRegistry;
        this.clock = clock;
        this.mapper = mapper;
    }

    @Override
    public Result<Void> validate(List<VerifiablePresentationContainer> presentations, Collection<? extends CredentialValidationRule> additionalRules) {
        return presentations.stream().map(verifiablePresentation -> {
            var credentials = verifiablePresentation.presentation().getCredentials();
            // verify, that the VP and all VPs are cryptographically OK
            var presentationIssuer = verifiablePresentation.presentation().getHolder();
            return presentationVerifier.verifyPresentation(verifiablePresentation)
                    .compose(u -> validateVerifiableCredentials(credentials, presentationIssuer, additionalRules));
        }).reduce(success(), Result::merge);
    }

    @NotNull
    private Result<Void> validateVerifiableCredentials(List<VerifiableCredential> credentials, String presentationHolder, Collection<? extends CredentialValidationRule> additionalRules) {

        // in addition, verify that all VCs are valid
        var filters = new ArrayList<>(List.of(
                new IsInValidityPeriod(clock),
                new HasValidSubjectIds(presentationHolder),
                new IsNotRevoked(revocationServiceRegistry),
                new HasValidIssuer(trustedIssuerRegistry),
                new HasValidSubjectSchema(mapper)));

        filters.addAll(additionalRules);

        if (credentials.isEmpty()) {
            return success();
        }

        return credentials
                .stream()
                .map(c -> filters.stream().reduce(t -> success(), CredentialValidationRule::and).apply(c))
                .reduce(Result::merge)
                .orElseGet(() -> failure("Could not determine the status of the VC validation"));
    }

}
