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

package org.eclipse.edc.connector.controlplane.api.management.protocolversion.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest.PROTOCOL_VERSION_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest.PROTOCOL_VERSION_REQUEST_COUNTER_PARTY_ID;
import static org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest.PROTOCOL_VERSION_REQUEST_PROTOCOL;

public class ProtocolVersionRequestValidator {

    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verify(PROTOCOL_VERSION_REQUEST_COUNTER_PARTY_ADDRESS, MandatoryValue::new)
                .verify(PROTOCOL_VERSION_REQUEST_COUNTER_PARTY_ID, MandatoryValue::new)
                .verify(PROTOCOL_VERSION_REQUEST_PROTOCOL, MandatoryValue::new)
                .build();
    }

}
