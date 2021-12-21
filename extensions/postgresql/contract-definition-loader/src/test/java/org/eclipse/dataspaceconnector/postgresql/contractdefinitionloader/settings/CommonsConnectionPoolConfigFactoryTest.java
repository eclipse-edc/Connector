/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.postgresql.contractdefinitionloader.settings;

import org.assertj.core.api.Assertions;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.commons.CommonsConnectionPoolConfig;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class CommonsConnectionPoolConfigFactoryTest {

    private static final int TOTAL_SETTINGS_COUNT = 8;

    private CommonsConnectionPoolConfigFactory factory;

    private Map<String, String> settingMap;

    // mocks
    private ServiceExtensionContext serviceExtensionContext;

    @BeforeEach
    public void setup() {
        settingMap = new HashMap<>();
        serviceExtensionContext = Mockito.mock(ServiceExtensionContext.class);
        Monitor monitor = Mockito.mock(Monitor.class);

        Mockito.when(serviceExtensionContext.getMonitor()).thenReturn(monitor);
        Mockito.when(serviceExtensionContext.getSetting(
                        Mockito.anyString(), Mockito.isNull()))
                .thenAnswer((a) -> {
                    String key = a.getArgument(0);
                    if (settingMap.containsKey(key)) {
                        return settingMap.get(key);
                    }
                    return null;
                });

        factory = new CommonsConnectionPoolConfigFactory(serviceExtensionContext);
    }

    @AfterEach
    public void tearDown() {
        Mockito.verify(serviceExtensionContext, Mockito.times(TOTAL_SETTINGS_COUNT))
                .getSetting(Mockito.anyString(), Mockito.isNull());
    }

    @Test
    public void testThrowsOnInvalidMaxIdleConnections() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_CONNECTION_POOL_MAX_IDLE_CONNECTIONS, "foo");

        // invoke & verify
        Assertions.assertThatThrownBy(() -> factory.create())
                .isInstanceOf(EdcException.class);
    }

    @Test
    public void testThrowsOnInvalidMinIdleConnections() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_CONNECTION_POOL_MIN_IDLE_CONNECTIONS, "foo");

        // invoke & verify
        Assertions.assertThatThrownBy(() -> factory.create())
                .isInstanceOf(EdcException.class);
    }

    @Test
    public void testThrowsOnInvalidMaxTotalConnections() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_CONNECTION_POOL_MAX_TOTAL_CONNECTIONS, "foo");

        // invoke & verify
        Assertions.assertThatThrownBy(() -> factory.create())
                .isInstanceOf(EdcException.class);
    }

    @Test
    public void testSuccess() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_CONNECTION_POOL_MAX_TOTAL_CONNECTIONS, "1");
        settingMap.put(SettingKeys.POSTGRESQL_CONNECTION_POOL_MIN_IDLE_CONNECTIONS, "1");
        settingMap.put(SettingKeys.POSTGRESQL_CONNECTION_POOL_MAX_IDLE_CONNECTIONS, "1");
        settingMap.put(SettingKeys.POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_ON_BORROW, "true");
        settingMap.put(SettingKeys.POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_ON_CREATE, "true");
        settingMap.put(SettingKeys.POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_ON_RETURN, "true");
        settingMap.put(SettingKeys.POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_WHILE_IDLE, "true");
        settingMap.put(SettingKeys.POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_QUERY, "SELECT foo FROM bar");

        // invoke
        CommonsConnectionPoolConfig config = factory.create();

        // verify
        Assertions.assertThat(config.getMaxIdleConnections()).isEqualTo(1);
        Assertions.assertThat(config.getMinIdleConnections()).isEqualTo(1);
        Assertions.assertThat(config.getMaxTotalConnections()).isEqualTo(1);
        Assertions.assertThat(config.getTestConnectionOnBorrow()).isEqualTo(true);
        Assertions.assertThat(config.getTestConnectionOnCreate()).isEqualTo(true);
        Assertions.assertThat(config.getTestConnectionOnReturn()).isEqualTo(true);
        Assertions.assertThat(config.getTestConnectionWhileIdle()).isEqualTo(true);
        Assertions.assertThat(config.getTestQuery()).isEqualTo("SELECT foo FROM bar");
    }
}
