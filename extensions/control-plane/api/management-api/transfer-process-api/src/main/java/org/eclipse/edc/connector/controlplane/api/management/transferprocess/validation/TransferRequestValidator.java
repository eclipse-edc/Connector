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

package org.eclipse.edc.connector.controlplane.api.management.transferprocess.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.api.validation.DataAddressValidator;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.LogDeprecatedValue;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.jsonobject.validators.OptionalIdNotBlank;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_ASSET_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_CONNECTOR_ADDRESS;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_CONTRACT_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_DATA_DESTINATION;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_PROTOCOL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TRANSFER_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TYPE;

public class TransferRequestValidator {

    public static Validator<JsonObject> instance(Monitor monitor) {
        return JsonObjectValidator.newValidator()
                .verifyId(OptionalIdNotBlank::new)
                .verify(MandatoryCounterPartyAddressOrConnectorAddress::new)
                .verify(TRANSFER_REQUEST_CONNECTOR_ADDRESS, path -> new LogDeprecatedValue(path, TRANSFER_REQUEST_TYPE, TRANSFER_REQUEST_COUNTER_PARTY_ADDRESS, monitor))
                .verify(TRANSFER_REQUEST_CONTRACT_ID, MandatoryValue::new)
                .verify(TRANSFER_REQUEST_PROTOCOL, MandatoryValue::new)
                .verify(TRANSFER_REQUEST_ASSET_ID, MandatoryValue::new)
                .verify(TRANSFER_REQUEST_TRANSFER_TYPE, MandatoryValue::new)
                .verifyObject(TRANSFER_REQUEST_DATA_DESTINATION, DataAddressValidator::instance)
                .build();
    }

    /**
     * This custom validator can be removed once `connectorAddress` is deleted and exists only for legacy reasons
     */
    private record MandatoryCounterPartyAddressOrConnectorAddress(JsonLdPath path) implements Validator<JsonObject> {

        @Override
        public ValidationResult validate(JsonObject input) {
            var counterPartyAddress = new MandatoryValue(path.append(TRANSFER_REQUEST_COUNTER_PARTY_ADDRESS));
            var validateCounterParty = counterPartyAddress.validate(input);
            if (validateCounterParty.succeeded()) {
                return ValidationResult.success();
            }
            var connectorAddress = new MandatoryValue(path.append(TRANSFER_REQUEST_CONNECTOR_ADDRESS));
            var validateConnectorAddress = connectorAddress.validate(input);
            if (validateConnectorAddress.succeeded()) {
                return ValidationResult.success();
            }
            return validateCounterParty;
        }
    }

}
