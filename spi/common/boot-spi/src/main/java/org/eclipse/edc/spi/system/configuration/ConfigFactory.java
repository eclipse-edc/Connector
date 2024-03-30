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

package org.eclipse.edc.spi.system.configuration;

import java.util.Map;
import java.util.Properties;

import static java.util.Collections.emptyMap;
import static org.eclipse.edc.spi.system.configuration.ConfigImpl.TO_MAP;

/**
 * Creates a {@link Config} from another data structure.
 */
public class ConfigFactory {

    /**
     * Returns an empty {@link Config}.
     *
     * @return an empty {@link Config}
     */
    public static Config empty() {
        return new ConfigImpl(emptyMap());
    }

    /**
     * Returns a config built from a {@link Map}.
     *
     * @return a {@link Config} instance based on the given {@link Map}
     */
    public static Config fromMap(Map<String, String> settings) {
        return new ConfigImpl(settings);
    }

    /**
     * Returns a config built from {@link Properties}.
     *
     * @return a {@link Config} instance based on the given {@link Properties}
     */
    public static Config fromProperties(Properties properties) {
        var entries = properties.entrySet().stream()
                .map(it -> Map.entry(it.getKey().toString(), it.getValue().toString()))
                .collect(TO_MAP);

        return new ConfigImpl(entries);
    }

    /**
     * Returns a config built from a {@link Map} containing environment variables, converting ENV_KEY_FORMAT to env.key.format
     * keys.
     *
     * @return a {@link Config} instance based on the given {@link Properties}
     */
    public static Config fromEnvironment(Map<String, String> environmentVariables) {
        var settings = environmentVariables.entrySet().stream()
                .map(it -> Map.entry(it.getKey().toLowerCase().replace("_", "."), it.getValue()))
                .collect(TO_MAP);

        return new ConfigImpl(settings);
    }
}
