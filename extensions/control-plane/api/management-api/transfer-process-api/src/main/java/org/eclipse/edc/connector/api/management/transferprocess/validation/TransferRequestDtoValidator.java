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

package org.eclipse.edc.connector.api.management.transferprocess.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.api.validation.DataAddressDtoValidator;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.jsonobject.validators.OptionalIdNotBlank;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_ASSET_ID;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_CONNECTOR_ADDRESS;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_CONNECTOR_ID;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_CONTRACT_ID;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_DATA_DESTINATION;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto.EDC_TRANSFER_REQUEST_DTO_PROTOCOL;

public class TransferRequestDtoValidator {

    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verifyId(OptionalIdNotBlank::new)
                .verify(EDC_TRANSFER_REQUEST_DTO_CONNECTOR_ADDRESS, MandatoryValue::new)
                .verify(EDC_TRANSFER_REQUEST_DTO_CONTRACT_ID, MandatoryValue::new)
                .verify(EDC_TRANSFER_REQUEST_DTO_PROTOCOL, MandatoryValue::new)
                .verify(EDC_TRANSFER_REQUEST_DTO_CONNECTOR_ID, MandatoryValue::new)
                .verify(EDC_TRANSFER_REQUEST_DTO_ASSET_ID, MandatoryValue::new)
                .verify(EDC_TRANSFER_REQUEST_DTO_DATA_DESTINATION, MandatoryObject::new)
                .verifyObject(EDC_TRANSFER_REQUEST_DTO_DATA_DESTINATION, DataAddressDtoValidator::instance)
                .build();
    }
}
