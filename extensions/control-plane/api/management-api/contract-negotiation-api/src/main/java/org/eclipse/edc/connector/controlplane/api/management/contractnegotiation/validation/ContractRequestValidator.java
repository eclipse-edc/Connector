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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryIdNotBlank;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.jsonobject.validators.TypeIs;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.POLICY;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.PROTOCOL;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ASSIGNER_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_OFFER;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;

public class ContractRequestValidator {
    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verify(CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS, MandatoryValue::new)
                .verify(PROTOCOL, MandatoryValue::new)
                .verify(POLICY, MandatoryObject::new)
                .verifyObject(POLICY, ContractRequestValidator::offerValidator)
                .build();
    }

    public static JsonObjectValidator.Builder offerValidator(JsonObjectValidator.Builder builder) {
        return builder
                .verifyId(MandatoryIdNotBlank::new)
                .verify(path -> new TypeIs(path, ODRL_POLICY_TYPE_OFFER))
                .verify(ODRL_ASSIGNER_ATTRIBUTE, MandatoryObject::new)
                .verifyObject(ODRL_ASSIGNER_ATTRIBUTE, b -> b.verifyId(MandatoryIdNotBlank::new))
                .verify(ODRL_TARGET_ATTRIBUTE, MandatoryObject::new)
                .verifyObject(ODRL_TARGET_ATTRIBUTE, b -> b.verifyId(MandatoryIdNotBlank::new));
    }

}
