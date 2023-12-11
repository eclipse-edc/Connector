/*
 *  Copyright (c) 2023 SAP Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       SAP Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.service.policydefinition;

import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.service.query.QueryValidator;
import org.eclipse.edc.spi.result.Result;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PolicyDefinitionQueryValidator extends QueryValidator {
    private static final Pattern VALID_QUERY_PATH_REGEX = Pattern.compile("^[A-Za-z_]+.*$");

    PolicyDefinitionQueryValidator(Map<Class<?>, List<Class<?>>> subtypeMap) {
        super(PolicyDefinition.class, subtypeMap);
    }

    /**
     * Only paths are valid that start with either a character or an '_'
     *
     * @param path The path. Cannot start with anything other chan A-Za-z_
     */
    @Override
    protected Result<Void> isValid(String path) {
        if (path.contains("privateProperties.") && VALID_QUERY_PATH_REGEX.matcher(path).matches()) {
            return Result.success();
        } else {
            return super.isValid(path);
        }
    }

}
