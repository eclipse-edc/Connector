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

import org.eclipse.edc.spi.dataaddress.DataAddressValidator;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;

import static org.eclipse.edc.spi.types.domain.HttpDataAddress.HTTP_DATA;

public class DataAddressValidatorImpl implements DataAddressValidator {

    @Override
    public Result<Void> validate(DataAddress dataAddress) {
        if (HTTP_DATA.equals(dataAddress.getType())) {
            return validateHttpDataAddress(dataAddress);
        } else {
            return Result.success();
        }
    }

    @NotNull
    private Result<Void> validateHttpDataAddress(DataAddress dataAddress) {
        var httpDataAddress = HttpDataAddress.Builder.newInstance()
                .properties(dataAddress.getProperties())
                .build();
        var baseUrl = httpDataAddress.getBaseUrl();
        try {
            new URL(baseUrl);
            return Result.success();
        } catch (MalformedURLException e) {
            return Result.failure("BaseUrl is not a valid URL: " + baseUrl);
        }
    }
}
