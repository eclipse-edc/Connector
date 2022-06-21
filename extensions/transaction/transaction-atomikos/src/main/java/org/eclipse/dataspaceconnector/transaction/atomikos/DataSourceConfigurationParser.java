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

package org.eclipse.dataspaceconnector.transaction.atomikos;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.CONNECTION_TIMEOUT;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.DRIVER_CLASS;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.DRIVER_PROPERTIES;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.DS_TYPE;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.LOGIN_TIMEOUT;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.MAINTENANCE_INTERVAL;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.MAX_IDLE;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.MAX_POOL_SIZE;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.MIN_POOL_SIZE;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.PASSWORD;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.POOL_SIZE;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.QUERY;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.REAP;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.URL;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.USERNAME;
import static org.eclipse.dataspaceconnector.transaction.atomikos.Setters.setIfProvided;
import static org.eclipse.dataspaceconnector.transaction.atomikos.Setters.setIfProvidedInt;

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
                            .driverClass(config.getString(DRIVER_CLASS))
                            .url(config.getString(URL));

                    setIfProvided(DS_TYPE, value -> setDataSourceType(keyName, builder, value), config);
                    setIfProvided(USERNAME, builder::username, config);
                    setIfProvided(PASSWORD, builder::password, config);
                    setIfProvidedInt(POOL_SIZE, builder::poolSize, config);
                    setIfProvidedInt(MAX_POOL_SIZE, builder::maxPoolSize, config);
                    setIfProvidedInt(MIN_POOL_SIZE, builder::minPoolSize, config);
                    setIfProvidedInt(CONNECTION_TIMEOUT, builder::connectionTimeout, config);
                    setIfProvidedInt(LOGIN_TIMEOUT, builder::loginTimeout, config);
                    setIfProvidedInt(MAINTENANCE_INTERVAL, builder::maintenanceInterval, config);
                    setIfProvidedInt(MAX_IDLE, builder::maxIdle, config);
                    setIfProvidedInt(REAP, builder::reap, config);
                    setIfProvided(QUERY, builder::query, config);

                    builder.properties(config.getRelativeEntries(DRIVER_PROPERTIES));

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    private static void setDataSourceType(String dsName, DataSourceConfiguration.Builder builder, String value) {
        try {
            var type = DataSourceConfiguration.DataSourceType.valueOf(value.toUpperCase(Locale.ROOT));
            builder.dataSourceType(type);
        } catch (IllegalArgumentException e) {
            throw new EdcException(format("Error configuring %s. Value must be XA or NON_XA for for %s: %s", dsName, DS_TYPE, value));
        }
    }

}
