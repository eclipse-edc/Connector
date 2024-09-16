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

package org.eclipse.edc.iam.verifiablecredentials.rules;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.spi.result.Result;

import java.util.Collections;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * A class that implements the {@link CredentialValidationRule} interface and checks if a {@link VerifiableCredential} has a trusted issuer,
 * and if the credential type is supported for this issuer.
 */
public class HasValidIssuer implements CredentialValidationRule {
    private final TrustedIssuerRegistry trustedIssuerRegistry;

    public HasValidIssuer(TrustedIssuerRegistry trustedIssuerRegistry) {
        this.trustedIssuerRegistry = trustedIssuerRegistry;
    }

    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        var issuer = credential.getIssuer();
        if (issuer.id() == null) {
            return failure("Issuer did not contain an 'id' field.");
        }

        var supportedTypes = trustedIssuerRegistry.getSupportedTypes(issuer);
        return !supportedTypes.contains(TrustedIssuerRegistry.WILDCARD) && Collections.disjoint(credential.getType(), supportedTypes) ?
                failure("Credential types '%s' are not supported for issuer '%s'".formatted(credential.getType(), issuer.id())) :
                success();
    }

}
