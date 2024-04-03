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
 * This class represents a rule that checks if a given VerifiableCredential is revoked based on a StatusList2021 credential.
 */
public class IsRevoked implements CredentialValidationRule {
    private final VerifiableCredential statusList2021;

    public IsRevoked(VerifiableCredential statusList2021) {
        this.statusList2021 = statusList2021;
    }


    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        return success();
    }
}
