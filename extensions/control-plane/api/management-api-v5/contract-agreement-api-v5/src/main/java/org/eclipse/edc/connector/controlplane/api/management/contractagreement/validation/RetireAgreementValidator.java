/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.contractagreement.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.RetireAgreement.RETIRE_AGREEMENT_REASON;

public class RetireAgreementValidator {

    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verify(RETIRE_AGREEMENT_REASON, MandatoryValue::new)
                .build();
    }
}
