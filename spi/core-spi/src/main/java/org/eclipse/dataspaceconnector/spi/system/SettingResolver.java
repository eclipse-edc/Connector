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

import org.eclipse.dataspaceconnector.spi.system.configuration.Config;

/**
 * Resolve config settings and parse them to different primitive types
 */
public interface SettingResolver {

    /**
     * Get the config for the specified path
     */
    Config getConfig(String path);

    /**
     * Get the config from the root
     */
    default Config getConfig() {
        return getConfig("");
    }

    /**
     * Returns the configuration value, or the default value if not found.
     *
     * @param setting the setting key
     * @param defaultValue value returned if no key found
     * @return the config value as int
     */
    default String getSetting(String setting, String defaultValue) {
        return getConfig().getString(setting, defaultValue);
    }

    /**
     * Returns the configuration value parsed as int or the default value if not found
     *
     * @param setting the setting key
     * @param defaultValue value returned if no key found
     * @return the config value as int
     */
    default int getSetting(String setting, int defaultValue) {
        return getConfig().getInteger(setting, defaultValue);
    }

    /**
     * Returns the configuration value parsed as long or the default value if not found
     *
     * @param setting the setting key
     * @param defaultValue value returned if no key found
     * @return the config value as long
     */
    default long getSetting(String setting, long defaultValue) {
        return getConfig().getLong(setting, defaultValue);
    }

}
