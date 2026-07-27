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

import org.eclipse.edc.iam.verifiablecredentials.rules.HasValidIssuer;
import org.eclipse.edc.iam.verifiablecredentials.rules.HasValidSubjectIds;
import org.eclipse.edc.iam.verifiablecredentials.spi.VerifiableCredentialValidationService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

public class VerifiableCredentialValidationServiceImpl implements VerifiableCredentialValidationService {
    private final PresentationVerifier presentationVerifier;
    private final List<CredentialValidationRule> baseRules;

    public VerifiableCredentialValidationServiceImpl(PresentationVerifier presentationVerifier, List<CredentialValidationRule> rules) {
        this.presentationVerifier = presentationVerifier;
        this.baseRules = rules;
    }

    @Override
    public Result<Void> validate(List<VerifiablePresentationContainer> presentations, String audience, DataspaceProfileContext dataspaceProfileContext) {
        if (presentations.isEmpty()) {
            return failure("No presentations to validate");
        }
        return presentations.stream()
                .map(verifiablePresentation -> validatePresentation(audience, verifiablePresentation, dataspaceProfileContext))
                .reduce(success(), Result::merge);
    }

    private Result<Void> validatePresentation(String audience, VerifiablePresentationContainer verifiablePresentation, DataspaceProfileContext dataspaceProfileContext) {
        var credentials = verifiablePresentation.presentation().getCredentials();
        // verify, that the VP and all VPs are cryptographically OK
        var presentationIssuer = verifiablePresentation.presentation().getHolder();
        return presentationVerifier.verifyPresentation(verifiablePresentation, audience)
                .compose(u -> validateVerifiableCredentials(credentials, presentationIssuer, dataspaceProfileContext));
    }

    @NotNull
    private Result<Void> validateVerifiableCredentials(List<VerifiableCredential> credentials, @Nullable String presentationHolder, DataspaceProfileContext dataspaceProfileContext) {
        var rules = new ArrayList<>(this.baseRules);
        rules.add(new HasValidIssuer(dataspaceProfileContext));

        if (presentationHolder != null) {
            rules.add(new HasValidSubjectIds(presentationHolder));
        }

        return credentials
                .stream()
                .map(c -> rules.stream().reduce(t -> success(), CredentialValidationRule::and).apply(c))
                .reduce(Result::merge)
                .orElseGet(Result::success);
    }

}
