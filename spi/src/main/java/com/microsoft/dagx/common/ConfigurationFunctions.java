/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.common;

/**
 * Common configuration functions used by extensions.
 */
public class ConfigurationFunctions {

    /**
     * Returns the property value, env value or default value for the key.
     * <p>
     * Naming conventions for keys are '[qualifier].[value]' in lower case. When checking for env variables, keys will be converted to uppercase and '.' replaced by '_'.
     */
    public static String propOrEnv(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        String upperKey = key.toUpperCase().replace('.', '_');
        value = System.getenv(upperKey);
        return value != null ? value : defaultValue;
    }
}
