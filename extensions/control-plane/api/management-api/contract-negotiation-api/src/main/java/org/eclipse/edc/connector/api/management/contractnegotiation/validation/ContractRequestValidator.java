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
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.ASSET_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.OFFER_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.POLICY;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.CONNECTOR_ADDRESS;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.OFFER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.PROTOCOL;

public class ContractRequestValidator {
    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verify(MandatoryCounterPartyAddressOrConnectorAddress::new)
                .verify(PROTOCOL, MandatoryValue::new)
                .verify(OFFER, MandatoryObject::new)
                .verifyObject(OFFER, v -> v
                        .verify(OFFER_ID, MandatoryValue::new)
                        .verify(ASSET_ID, MandatoryValue::new)
                        .verify(POLICY, MandatoryObject::new)
                )
                .build();
    }

    private record MandatoryCounterPartyAddressOrConnectorAddress(JsonLdPath path) implements Validator<JsonObject> {

        @Override
        public ValidationResult validate(JsonObject input) {
            var counterPartyAddress = new MandatoryValue(path.append(CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS));
            var connectorAddress = new MandatoryValue(path.append(CONNECTOR_ADDRESS));

            var validateCounterParty = counterPartyAddress.validate(input);
            var validateConnectorAddress = connectorAddress.validate(input);

            if (validateCounterParty.succeeded() || validateConnectorAddress.succeeded()) {
                return ValidationResult.success();
            } else {
                return validateCounterParty;
            }
        }
    }
}
