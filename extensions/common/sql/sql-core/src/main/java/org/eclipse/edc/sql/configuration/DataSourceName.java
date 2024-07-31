/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.sql.configuration;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.Config;

import static org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry.DEFAULT_DATASOURCE;

/**
 * This interface is only functional to the migration from the old format to the new one.
 * Ref. <a href="https://github.com/eclipse-edc/Connector/issues/3811">https://github.com/eclipse-edc/Connector/issues/3811</a>
 */
public interface DataSourceName {

    /**
     * Extracts datasource name from configuration considering deprecated key as fallback.
     *
     * @param key the setting key.
     * @param deprecatedKey the deprecate setting key.
     * @param config the config.
     * @param monitor the monitor.
     * @return the datasource name
     * @deprecated will be removed together with the deprecated settings.
     */
    @Deprecated(since = "0.8.1")
    static String getDataSourceName(String key, String deprecatedKey, Config config, Monitor monitor) {
        var datasourceName = config.getString(key, null);

        if (datasourceName != null) {
            return datasourceName;
        }

        var name = config.getString(deprecatedKey, null);

        if (name != null) {
            monitor.warning("Datasource name setting key %s has been deprecated, please switch to %s".formatted(deprecatedKey, key));
            return name;
        }

        return DEFAULT_DATASOURCE;
    }

}
