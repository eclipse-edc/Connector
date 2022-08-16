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

package org.eclipse.dataspaceconnector.spi.system.configuration;

import java.util.Map;
import java.util.Properties;

import static java.util.Collections.emptyMap;
import static org.eclipse.dataspaceconnector.spi.system.configuration.ConfigImpl.TO_MAP;

/**
 * Creates a {@link Config} from another data structure.
 */
public class ConfigFactory {

    public static Config empty() {
        return new ConfigImpl(emptyMap());
    }

    public static Config fromMap(Map<String, String> propertyCache) {
        return new ConfigImpl(propertyCache);
    }

    public static Config fromProperties(Properties properties) {
        var entries = properties.entrySet().stream()
                .map(it -> Map.entry(it.getKey().toString(), it.getValue().toString()))
                .collect(TO_MAP);

        return new ConfigImpl(entries);
    }
}
