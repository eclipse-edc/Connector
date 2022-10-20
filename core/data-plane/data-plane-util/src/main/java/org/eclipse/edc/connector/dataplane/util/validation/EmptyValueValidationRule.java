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
import org.eclipse.edc.util.string.StringUtils;

import java.util.Map;

public class EmptyValueValidationRule implements ValidationRule<Map<String, String>> {

    private final String keyName;

    public EmptyValueValidationRule(String keyName) {
        this.keyName = keyName;
    }

    @Override
    public Result<Void> apply(Map<String, String> map) {
        return StringUtils.isNullOrBlank(map.get(keyName))
                ? Result.failure("Missing or invalid value for key " + keyName)
                : Result.success();
    }
}
