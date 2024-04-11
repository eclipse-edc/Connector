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

package org.eclipse.edc.iam.verifiablecredentials.spi;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.spi.result.Result;

import java.util.Collection;
import java.util.List;

public interface VerifiableCredentialValidationService {

    default Result<Void> validate(List<VerifiablePresentationContainer> presentations, CredentialValidationRule... additionalValidations) {
        return validate(presentations, List.of(additionalValidations));
    }

    Result<Void> validate(List<VerifiablePresentationContainer> presentations, Collection<? extends CredentialValidationRule> additionalValidations);
}
