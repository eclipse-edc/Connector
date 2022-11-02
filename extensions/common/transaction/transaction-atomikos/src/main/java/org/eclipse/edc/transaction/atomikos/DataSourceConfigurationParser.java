/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.transaction.atomikos;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.eclipse.edc.transaction.atomikos.Setters.setIfProvided;
import static org.eclipse.edc.transaction.atomikos.Setters.setIfProvidedInt;

/**
 * Bootstraps the Atomikos {@link DataSourceRegistry}.
 */
public class DataSourceConfigurationParser {

    public static List<DataSourceConfiguration> parseDataSourceConfigurations(Config parent) {
        return parent.partition()
                .map(config -> {
                    var dsName = config.currentNode();
                    var keyName = "data source " + dsName;
                    var builder = DataSourceConfiguration.Builder.newInstance()
                            .name(dsName)
                            .driverClass(config.getString(DataSourceConfigurationKeys.DRIVER_CLASS))
                            .url(config.getString(DataSourceConfigurationKeys.URL));

                    setIfProvided(DataSourceConfigurationKeys.DS_TYPE, value -> setDataSourceType(keyName, builder, value), config);
                    setIfProvided(DataSourceConfigurationKeys.USERNAME, builder::username, config);
                    setIfProvided(DataSourceConfigurationKeys.PASSWORD, builder::password, config);
                    setIfProvidedInt(DataSourceConfigurationKeys.POOL_SIZE, builder::poolSize, config);
                    setIfProvidedInt(DataSourceConfigurationKeys.MAX_POOL_SIZE, builder::maxPoolSize, config);
                    setIfProvidedInt(DataSourceConfigurationKeys.MIN_POOL_SIZE, builder::minPoolSize, config);
                    setIfProvidedInt(DataSourceConfigurationKeys.CONNECTION_TIMEOUT, builder::connectionTimeout, config);
                    setIfProvidedInt(DataSourceConfigurationKeys.LOGIN_TIMEOUT, builder::loginTimeout, config);
                    setIfProvidedInt(DataSourceConfigurationKeys.MAINTENANCE_INTERVAL, builder::maintenanceInterval, config);
                    setIfProvidedInt(DataSourceConfigurationKeys.MAX_IDLE, builder::maxIdle, config);
                    setIfProvidedInt(DataSourceConfigurationKeys.REAP, builder::reap, config);
                    setIfProvided(DataSourceConfigurationKeys.QUERY, builder::query, config);

                    builder.properties(config.getRelativeEntries(DataSourceConfigurationKeys.DRIVER_PROPERTIES));

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    private static void setDataSourceType(String dsName, DataSourceConfiguration.Builder builder, String value) {
        try {
            var type = DataSourceConfiguration.DataSourceType.valueOf(value.toUpperCase(Locale.ROOT));
            builder.dataSourceType(type);
        } catch (IllegalArgumentException e) {
            throw new EdcException(String.format("Error configuring %s. Value must be XA or NON_XA for for %s: %s", dsName, DataSourceConfigurationKeys.DS_TYPE, value));
        }
    }

}
