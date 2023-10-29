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

package org.eclipse.edc.validator.dataaddress.httpdata;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.net.MalformedURLException;
import java.net.URL;

import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.BASE_URL;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.HTTP_DATA_TYPE;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validator for HttpData DataAddress type
 */
public class HttpDataDataAddressValidator implements Validator<DataAddress> {

    @Override
    public ValidationResult validate(DataAddress dataAddress) {
        var baseUrl = dataAddress.getStringProperty(BASE_URL);
        try {
            new URL(baseUrl);
            return ValidationResult.success();
        } catch (MalformedURLException e) {
            var violation = violation("DataAddress of type %s must contain a valid baseUrl.".formatted(HTTP_DATA_TYPE), BASE_URL, baseUrl);
            return ValidationResult.failure(violation);
        }
    }
}
