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

package org.eclipse.edc.iam.identitytrust.validation;

import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.validation.VcValidationRule;
import org.eclipse.edc.spi.result.Result;

import static org.eclipse.edc.spi.result.Result.success;

public class IsRevoked implements VcValidationRule {
    private final VerifiableCredential statusList2021;

    public IsRevoked(VerifiableCredential statusList2021) {
        this.statusList2021 = statusList2021;
    }


    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        return success();
    }
}
