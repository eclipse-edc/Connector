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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

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
        var defaultProperties = new HashMap<String, String>();
        defaultProperties.put(DRIVER_CLASS, "com.Driver");
        defaultProperties.put(URL, "jdbc://foo.com");
        defaultProperties.put(DS_TYPE, "xa");
        defaultProperties.put(USERNAME, "username");
        defaultProperties.put(PASSWORD, "password");
        defaultProperties.put(POOL_SIZE, "1");
        defaultProperties.put(MAX_POOL_SIZE, "1");
        defaultProperties.put(MIN_POOL_SIZE, "1");
        defaultProperties.put(CONNECTION_TIMEOUT, "1");
        defaultProperties.put(LOGIN_TIMEOUT, "1");
        defaultProperties.put(MAINTENANCE_INTERVAL, "1");
        defaultProperties.put(MAX_IDLE, "1");
        defaultProperties.put(REAP, "1");
        defaultProperties.put(QUERY, "SELECT");
        defaultProperties.put(DRIVER_PROPERTIES + ".custom", "customvalue");

        var fooProperties = Map.of(DRIVER_CLASS, "com.Driver", URL, "jdbc://foo.com");

        var parsedConfigurations = DataSourceConfigurationParser.parseDataSourceConfigurations(Map.of("default", defaultProperties, "minimal", fooProperties));

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
