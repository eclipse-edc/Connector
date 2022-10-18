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

package org.eclipse.dataspaceconnector.aws.dataplane.s3.validation;

import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.spi.result.Result;

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
