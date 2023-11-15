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

package org.eclipse.edc.connector.api.management.contractnegotiation.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import static java.lang.String.format;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.ASSET_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.OFFER_ID;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.CONNECTOR_ADDRESS;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_TYPE;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.OFFER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.POLICY;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.PROTOCOL;

public class ContractRequestValidator {

    public static Validator<JsonObject> instance(Monitor monitor) {
        return JsonObjectValidator.newValidator()
                .verify(CONNECTOR_ADDRESS, MandatoryValue::new)
                .verify(PROTOCOL, MandatoryValue::new)
                .verify(path -> new MandatoryOfferOrPolicy(path, monitor))
                .build();
    }

    private record MandatoryOfferOrPolicy(JsonLdPath path, Monitor monitor) implements Validator<JsonObject> {
        @Override
        public ValidationResult validate(JsonObject input) {
            var offerValidity = new MandatoryObject(path.append(OFFER)).validate(input);
            if (offerValidity.succeeded()) {
                monitor.warning(format("The attribute %s has been deprecated in type %s, please use %s",
                        OFFER, CONTRACT_REQUEST_TYPE, POLICY));
                return JsonObjectValidator.newValidator()
                        .verifyObject(OFFER, v -> v
                                .verify(OFFER_ID, MandatoryValue::new)
                                .verify(ASSET_ID, MandatoryValue::new)
                                .verify(ContractOfferDescription.POLICY, MandatoryObject::new)
                        ).build().validate(input);
            }

            var policyValidity = new MandatoryObject(path.append(POLICY)).validate(input);
            if (policyValidity.succeeded()) {
                return ValidationResult.success();
            }

            return ValidationResult.failure(Violation.violation(format("'%s' or '%s' must not be empty", OFFER, POLICY), path.toString()));
        }
    }
}
