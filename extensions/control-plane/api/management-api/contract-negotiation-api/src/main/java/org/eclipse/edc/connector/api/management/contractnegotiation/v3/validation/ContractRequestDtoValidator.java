/*
 *  Copyright (c) 2023 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Negotiation API enhancement
 *
 */

package org.eclipse.edc.connector.api.management.contractnegotiation.v3.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.connector.api.management.contractnegotiation.v3.model.ContractRequestDto.ASSET_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.v3.model.ContractRequestDto.CONNECTOR_ADDRESS;
import static org.eclipse.edc.connector.api.management.contractnegotiation.v3.model.ContractRequestDto.POLICY;
import static org.eclipse.edc.connector.api.management.contractnegotiation.v3.model.ContractRequestDto.PROTOCOL;

public class ContractRequestDtoValidator {
    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verify(CONNECTOR_ADDRESS, MandatoryValue::new)
                .verify(PROTOCOL, MandatoryValue::new)
                .verify(ASSET_ID, MandatoryObject::new)
                .verify(POLICY, MandatoryObject::new)
                .build();
    }
}
