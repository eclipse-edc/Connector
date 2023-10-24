/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.service.dataaddress;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.validator.spi.DataAddressValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;

import static org.eclipse.edc.spi.types.domain.HttpDataAddress.HTTP_DATA;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class DataAddressValidatorImpl implements DataAddressValidator {

    @Override
    public ValidationResult validate(DataAddress dataAddress) {
        if (HTTP_DATA.equals(dataAddress.getType())) {
            return validateHttpDataAddress(dataAddress);
        } else {
            return ValidationResult.success();
        }
    }

    @NotNull
    private ValidationResult validateHttpDataAddress(DataAddress dataAddress) {
        var httpDataAddress = HttpDataAddress.Builder.newInstance()
                .properties(dataAddress.getProperties())
                .build();
        var baseUrl = httpDataAddress.getBaseUrl();
        try {
            new URL(baseUrl);
            return ValidationResult.success();
        } catch (MalformedURLException e) {
            var violation = violation("DataAddress of type %s must contain a valid baseUrl: %s".formatted(HTTP_DATA, baseUrl), "baseUrl", baseUrl);
            return ValidationResult.failure(violation);
        }
    }
}
