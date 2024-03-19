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
                            .driverClass(config.getString(AtomikosTransactionExtension.DRIVER_CLASS))
                            .url(config.getString(AtomikosTransactionExtension.URL));

                    setIfProvided(AtomikosTransactionExtension.DS_TYPE, value -> setDataSourceType(keyName, builder, value), config);
                    setIfProvided(AtomikosTransactionExtension.USERNAME, builder::username, config);
                    setIfProvided(AtomikosTransactionExtension.PASSWORD, builder::password, config);
                    setIfProvidedInt(AtomikosTransactionExtension.POOL_SIZE, builder::poolSize, config);
                    setIfProvidedInt(AtomikosTransactionExtension.MAX_POOL_SIZE, builder::maxPoolSize, config);
                    setIfProvidedInt(AtomikosTransactionExtension.MIN_POOL_SIZE, builder::minPoolSize, config);
                    setIfProvidedInt(AtomikosTransactionExtension.CONNECTION_TIMEOUT, builder::connectionTimeout, config);
                    setIfProvidedInt(AtomikosTransactionExtension.LOGIN_TIMEOUT, builder::loginTimeout, config);
                    setIfProvidedInt(AtomikosTransactionExtension.MAINTENANCE_INTERVAL, builder::maintenanceInterval, config);
                    setIfProvidedInt(AtomikosTransactionExtension.MAX_IDLE, builder::maxIdle, config);
                    setIfProvided(AtomikosTransactionExtension.QUERY, builder::query, config);

                    builder.properties(config.getRelativeEntries(AtomikosTransactionExtension.DRIVER_PROPERTIES));

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    private static void setDataSourceType(String dsName, DataSourceConfiguration.Builder builder, String value) {
        try {
            var type = DataSourceConfiguration.DataSourceType.valueOf(value.toUpperCase(Locale.ROOT));
            builder.dataSourceType(type);
        } catch (IllegalArgumentException e) {
            throw new EdcException(String.format("Error configuring %s. Value must be XA or NON_XA for for %s: %s", dsName, AtomikosTransactionExtension.DS_TYPE, value));
        }
    }

}
