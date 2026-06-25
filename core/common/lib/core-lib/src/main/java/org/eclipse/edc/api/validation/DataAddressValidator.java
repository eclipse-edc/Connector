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

package org.eclipse.edc.api.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY;

public class DataAddressValidator {
    public static Validator<JsonObject> instance() {
        return instance(JsonObjectValidator.newValidator()).build();
    }

    public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
        return builder
                .verify(EDC_DATA_ADDRESS_TYPE_PROPERTY, MandatoryValue::new);
    }
}
