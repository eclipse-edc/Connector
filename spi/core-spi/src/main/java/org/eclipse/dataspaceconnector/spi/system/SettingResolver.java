/*
 *  Copyright (c) 2021 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.spi.system;

import org.eclipse.dataspaceconnector.spi.EdcException;

import java.util.Map;

import static java.lang.String.format;

/**
 * Resolve config settings and parse them to different primitive types
 */
public interface SettingResolver {

    /**
     * Returns the configuration value, or the default value if not found.
     *
     * @param setting the setting key
     * @param defaultValue value returned if no key found
     * @return the config value as int
     */
    String getSetting(String setting, String defaultValue);

    /**
     * Gets all properties that start with a particular prefix and returns them as a map.
     */
    Map<String, Object> getSettings(String prefix);

    /**
     * Returns the configuration value parsed as int or the default value if not found
     *
     * @param setting the setting key
     * @param defaultValue value returned if no key found
     * @return the config value as int
     */
    default int getSetting(String setting, int defaultValue) {
        String value = getSetting(setting, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new EdcException(format("Value for setting %s is not a valid integer: %s", setting, value), e);
        }
    }

    /**
     * Returns the configuration value parsed as long or the default value if not found
     *
     * @param setting the setting key
     * @param defaultValue value returned if no key found
     * @return the config value as long
     */
    default long getSetting(String setting, long defaultValue) {
        String value = getSetting(setting, String.valueOf(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new EdcException(format("Value for setting %s is not a valid long: %s", setting, value), e);
        }
    }

}
