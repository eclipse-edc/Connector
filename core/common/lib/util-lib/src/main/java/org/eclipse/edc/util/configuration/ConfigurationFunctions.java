/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.util.configuration;

import org.eclipse.edc.util.string.StringUtils;

/**
 * Common configuration functions used by extensions.
 */
public class ConfigurationFunctions {

    /**
     * Returns the property value, env value or default value for the key.
     * <p>Naming conventions for keys are '[qualifier].[value]' in lower case. When checking for env variables, keys will be converted to uppercase and '.' replaced by '_'.</p>
     */
    public static String propOrEnv(String key, String defaultValue) {
        var value = System.getProperty(key);
        if (!StringUtils.isNullOrBlank(value)) {
            return value;
        }
        var upperKey = key.toUpperCase().replace('.', '_');
        value = System.getenv(upperKey);
        if (!StringUtils.isNullOrBlank(value)) {
            return value;
        }
        return defaultValue;
    }

}
