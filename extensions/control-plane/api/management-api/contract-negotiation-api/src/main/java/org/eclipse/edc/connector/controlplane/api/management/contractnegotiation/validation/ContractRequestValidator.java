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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.LogDeprecatedValue;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryIdNotBlank;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.jsonobject.validators.TypeIs;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.model.ContractOfferDescription.ASSET_ID;
import static org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.model.ContractOfferDescription.OFFER_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONNECTOR_ADDRESS;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_TYPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.OFFER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.POLICY;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.PROTOCOL;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.PROVIDER_ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ASSIGNER_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_OFFER;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;

public class ContractRequestValidator {
    public static Validator<JsonObject> instance(Monitor monitor) {
        return JsonObjectValidator.newValidator()
                .verify(path -> new LogDeprecatedValue(path.append(PROVIDER_ID), CONTRACT_REQUEST_TYPE, ODRL_ASSIGNER_ATTRIBUTE, monitor))
                .verify(path -> new LogDeprecatedValue(path.append(OFFER), CONTRACT_REQUEST_TYPE, POLICY, monitor))
                .verify(path -> new LogDeprecatedValue(path.append(CONNECTOR_ADDRESS), CONTRACT_REQUEST_TYPE, CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS, monitor))
                .verify(path -> new MandatoryCounterPartyAddressOrConnectorAddress(path, monitor))
                .verify(PROTOCOL, MandatoryValue::new)
                .verify(path -> new MandatoryOfferOrPolicy(path, monitor))
                .build();
    }

    private record MandatoryOfferOrPolicy(JsonLdPath path, Monitor monitor) implements Validator<JsonObject> {
        @Override
        public ValidationResult validate(JsonObject input) {
            var offerValidity = new MandatoryObject(path.append(OFFER)).validate(input);
            if (offerValidity.succeeded()) {
                return JsonObjectValidator.newValidator()
                        .verifyObject(OFFER, v -> v
                                .verify(OFFER_ID, MandatoryValue::new)
                                .verify(ASSET_ID, MandatoryValue::new)
                                .verify(ContractOfferDescription.POLICY, MandatoryObject::new)
                        ).build().validate(input);
            }

            var validator = JsonObjectValidator.newValidator()
                    .verify(POLICY, MandatoryObject::new)
                    .verifyObject(POLICY, builder -> builder
                            .verifyId(MandatoryIdNotBlank::new)
                            .verify(path -> new TypeIs(path, ODRL_POLICY_TYPE_OFFER))
                            .verify(ODRL_ASSIGNER_ATTRIBUTE, MandatoryObject::new)
                            .verifyObject(ODRL_ASSIGNER_ATTRIBUTE, b -> b.verifyId(MandatoryIdNotBlank::new))
                            .verify(ODRL_TARGET_ATTRIBUTE, MandatoryObject::new)
                            .verifyObject(ODRL_TARGET_ATTRIBUTE, b -> b.verifyId(MandatoryIdNotBlank::new))
                    )
                    .build();

            return validator.validate(input);
        }
    }

    /**
     * This custom validator can be removed once `connectorAddress` is deleted and exists only for legacy reasons
     */
    private record MandatoryCounterPartyAddressOrConnectorAddress(JsonLdPath path, Monitor monitor) implements Validator<JsonObject> {

        @Override
        public ValidationResult validate(JsonObject input) {
            var counterPartyAddress = new MandatoryValue(path.append(CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS));
            var validateCounterParty = counterPartyAddress.validate(input);
            if (validateCounterParty.succeeded()) {
                return ValidationResult.success();
            }
            var connectorAddress = new MandatoryValue(path.append(CONNECTOR_ADDRESS));
            var validateConnectorAddress = connectorAddress.validate(input);
            if (validateConnectorAddress.succeeded()) {
                return ValidationResult.success();
            }
            return validateCounterParty;
        }
    }
}
