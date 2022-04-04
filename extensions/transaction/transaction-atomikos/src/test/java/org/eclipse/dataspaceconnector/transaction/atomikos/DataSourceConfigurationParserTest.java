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
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfiguration.DataSourceType.XA;
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

class DataSourceConfigurationParserTest {

    @Test
    void verifyCreation() {
        var properties = new HashMap<String, String>();
        properties.put("default." + DRIVER_CLASS, "com.Driver");
        properties.put("default." + URL, "jdbc://foo.com");
        properties.put("default." + DS_TYPE, "xa");
        properties.put("default." + USERNAME, "username");
        properties.put("default." + PASSWORD, "password");
        properties.put("default." + POOL_SIZE, "1");
        properties.put("default." + MAX_POOL_SIZE, "1");
        properties.put("default." + MIN_POOL_SIZE, "1");
        properties.put("default." + CONNECTION_TIMEOUT, "1");
        properties.put("default." + LOGIN_TIMEOUT, "1");
        properties.put("default." + MAINTENANCE_INTERVAL, "1");
        properties.put("default." + MAX_IDLE, "1");
        properties.put("default." + REAP, "1");
        properties.put("default." + QUERY, "SELECT");
        properties.put("default." + DRIVER_PROPERTIES + ".custom", "customvalue");
        properties.put("minimal." + DRIVER_CLASS, "com.Driver");
        properties.put("minimal." + URL, "jdbc://foo.com");

        Config config = ConfigFactory.fromMap(properties);
        var parsedConfigurations = DataSourceConfigurationParser.parseDataSourceConfigurations(config);

        assertThat(parsedConfigurations.size()).isEqualTo(2);
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        var defaultConfiguration = parsedConfigurations.stream().filter(c -> "default".equals(c.getName())).findFirst().get();

        assertThat(defaultConfiguration.getDriverClass()).isEqualTo("com.Driver");
        assertThat(defaultConfiguration.getUrl()).isEqualTo("jdbc://foo.com");
        assertThat(defaultConfiguration.getDataSourceType()).isEqualTo(XA);
        assertThat(defaultConfiguration.getUsername()).isEqualTo("username");
        assertThat(defaultConfiguration.getPassword()).isEqualTo("password");
        assertThat(defaultConfiguration.getPoolSize()).isEqualTo(1);
        assertThat(defaultConfiguration.getMaxPoolSize()).isEqualTo(1);
        assertThat(defaultConfiguration.getMinPoolSize()).isEqualTo(1);
        assertThat(defaultConfiguration.getConnectionTimeout()).isEqualTo(1);
        assertThat(defaultConfiguration.getLoginTimeout()).isEqualTo(1);
        assertThat(defaultConfiguration.getMaintenanceInterval()).isEqualTo(1);
        assertThat(defaultConfiguration.getMaxIdle()).isEqualTo(1);
        assertThat(defaultConfiguration.getReap()).isEqualTo(1);
        assertThat(defaultConfiguration.getQuery()).isEqualTo("SELECT");
        assertThat(defaultConfiguration.getProperties()).containsEntry(DRIVER_PROPERTIES + ".custom", "customvalue");

        // verify case where only minimal config is supplied
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        var minimalConfiguration = parsedConfigurations.stream().filter(c -> "minimal".equals(c.getName())).findFirst().get();
        assertThat(minimalConfiguration.getDriverClass()).isEqualTo("com.Driver");
        assertThat(minimalConfiguration.getUrl()).isEqualTo("jdbc://foo.com");
    }
}
