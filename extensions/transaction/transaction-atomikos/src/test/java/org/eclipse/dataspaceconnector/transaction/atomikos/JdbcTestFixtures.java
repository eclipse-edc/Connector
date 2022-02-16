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

import org.eclipse.dataspaceconnector.spi.system.configuration.Config;
import org.eclipse.dataspaceconnector.spi.system.configuration.ConfigFactory;

import java.util.HashMap;

import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfiguration.DataSourceType.NON_XA;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.CONNECTION_TIMEOUT;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.DRIVER_CLASS;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.DS_TYPE;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.LOGIN_TIMEOUT;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationKeys.URL;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.LOGGING;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.TIMEOUT;

/**
 * Test fixtures for working with JDBC resources.
 */
public class JdbcTestFixtures {

    public static Config createDataSourceConfig() {
        var properties = new HashMap<String, String>();
        var url = "jdbc:h2:mem:mydatabase;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=TRUE";
        var driverClass = "org.h2.Driver";
        properties.put("default." + URL, url);
        properties.put("default." + DRIVER_CLASS, driverClass);
        properties.put("default." + DS_TYPE, NON_XA.toString().toLowerCase());
        properties.put("default." + DataSourceConfigurationKeys.POOL_SIZE, "2");
        return ConfigFactory.fromMap(properties);
    }

    public static Config createAtomikosConfig() {
        var properties = new HashMap<String, String>();
        properties.put(TIMEOUT, "60");
        return ConfigFactory.fromMap(properties);
    }
}
