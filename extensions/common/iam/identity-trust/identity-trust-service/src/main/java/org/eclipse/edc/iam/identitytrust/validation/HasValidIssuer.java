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

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

public class HasValidIssuer implements VcValidationRule {
    private final List<String> allowedIssuers;

    public HasValidIssuer(List<String> allowedIssuers) {

        this.allowedIssuers = allowedIssuers;
    }

    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        var issuerObject = credential.getIssuer();
        String issuer;
        // issuers can be URLs, or Objects containing an "id" property
        if (issuerObject instanceof String) {
            issuer = issuerObject.toString();
        } else if (issuerObject instanceof Map) {
            issuer = ((Map) issuerObject).get("id").toString();
        } else {
            return failure("VC Issuer must either be a String or an Object but was %s.".formatted(issuerObject.getClass()));
        }

        return allowedIssuers.contains(issuer) ? success() : failure("Issuer '%s' is not in the list of allowed issuers".formatted(issuer));
    }
}
