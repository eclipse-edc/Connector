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
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import static org.eclipse.dataspaceconnector.transaction.atomikos.Setters.setMandatory;

/**
 * Bootstraps the Atomikos {@link DataSourceRegistry}.
 */
public class DataSourceConfigurationParser {

    public static List<DataSourceConfiguration> parseDataSourceConfigurations(Map<String, Map<String, Object>> configurations) {
        var dataSourceConfigurations = new ArrayList<DataSourceConfiguration>();
        configurations.forEach((dsName, properties) -> {
            var builder = DataSourceConfiguration.Builder.newInstance();
            var keyName = "data source " + dsName;
            builder.name(dsName);
            setMandatory(DRIVER_CLASS, keyName, builder::driverClass, properties);
            setMandatory(URL, keyName, builder::url, properties);
            setIfProvided(DS_TYPE, value -> setDataSourceType(keyName, builder, value), properties);
            setIfProvided(USERNAME, builder::username, properties);
            setIfProvided(PASSWORD, builder::password, properties);
            setIfProvidedInt(POOL_SIZE, keyName, builder::poolSize, properties);
            setIfProvidedInt(MAX_POOL_SIZE, keyName, builder::maxPoolSize, properties);
            setIfProvidedInt(MIN_POOL_SIZE, keyName, builder::minPoolSize, properties);
            setIfProvidedInt(CONNECTION_TIMEOUT, keyName, builder::connectionTimeout, properties);
            setIfProvidedInt(LOGIN_TIMEOUT, keyName, builder::loginTimeout, properties);
            setIfProvidedInt(MAINTENANCE_INTERVAL, keyName, builder::maintenanceInterval, properties);
            setIfProvidedInt(MAX_IDLE, keyName, builder::maxIdle, properties);
            setIfProvidedInt(REAP, keyName, builder::reap, properties);
            setIfProvided(QUERY, builder::query, properties);

            var driverProperties = properties.entrySet().stream()
                    .filter(e -> e.getKey().startsWith(DRIVER_PROPERTIES + "."))
                    .collect(Collectors.toMap(Map.Entry::getKey, stringObjectEntry -> stringObjectEntry.getValue().toString()));
            builder.properties(driverProperties);

            dataSourceConfigurations.add(builder.build());
        });
        return dataSourceConfigurations;
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
