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

import org.eclipse.edc.iam.identitytrust.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.identitytrust.spi.validation.CredentialValidationRule;
import org.eclipse.edc.spi.result.Result;

import java.time.Clock;

public class IsNotExpired implements CredentialValidationRule {
    private final Clock clock;

    public IsNotExpired(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        // issuance date can not be null, due to builder validation
        return credential.getIssuanceDate().isAfter(clock.instant()) ?
                Result.failure("Credential is not yet valid.") :
                Result.success();
    }
}
