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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.connector.dataplane.util.validation;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.util.string.StringUtils;

public class EmptyValueValidationRule implements ValidationRule<DataAddress> {

    private final String keyName;

    public EmptyValueValidationRule(String keyName) {
        this.keyName = keyName;
    }

    @Override
    public Result<Void> apply(DataAddress dataAddress) {
        return StringUtils.isNullOrBlank(dataAddress.getStringProperty(keyName))
                ? Result.failure("Missing or invalid value for key " + keyName)
                : Result.success();
    }
}
