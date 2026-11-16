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

package org.eclipse.edc.iam.verifiablecredentials.spi.validation;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.result.Result;

import java.util.function.Function;

/**
 * Interface for a rule that is being applied to a {@link VerifiableCredential}, and returns {@link Result#success()} or {@link Result#failure(String)}.
 */
public interface CredentialValidationRule extends Function<VerifiableCredential, Result<Void>> {

    /**
     * Combines this rule with another one: both are applied, and the failure messages of both are reported. The
     * combined failure is a {@link org.eclipse.edc.spi.result.RetryableFailure} only if every failed rule could not be
     * checked (e.g. a status list could not be downloaded), see {@link Result#merge(Result)}.
     */
    default CredentialValidationRule and(CredentialValidationRule other) {
        return t -> this.apply(t).merge(other.apply(t));
    }
}
