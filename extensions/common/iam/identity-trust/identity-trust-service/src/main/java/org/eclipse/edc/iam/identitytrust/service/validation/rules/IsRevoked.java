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

package org.eclipse.edc.iam.identitytrust.service.validation.rules;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.spi.result.Result;

import static org.eclipse.edc.spi.result.Result.success;

/**
 * This class represents a rule that checks if a given VerifiableCredential is revoked based on a BitStringStatusList credential.
 */
public class IsRevoked implements CredentialValidationRule {

    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        if (credential.getCredentialStatus().isEmpty()) {
            return success();
        }

        var credentialStatus = credential.getCredentialStatus();
        credentialStatus.forEach(status -> {
            
        });
        return Result.success(); //todo: implement
    }
}
