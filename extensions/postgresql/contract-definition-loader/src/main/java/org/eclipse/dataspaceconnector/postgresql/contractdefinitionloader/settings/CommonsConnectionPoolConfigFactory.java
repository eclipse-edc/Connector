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

import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.commons.CommonsConnectionPoolConfig;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CommonsConnectionPoolConfigFactory {

    private static final String LOG_INVALID_NUMBER = "AssetLoader-ConnectionPool-Configuration: Expecting number in property %s";
    private static final String EXCEPTION_INVALID_SETTINGS = "AssetLoader-ConnectionPool-Configuration: Invalid ConnectionPool settings.";

    private final ServiceExtensionContext serviceExtensionContext;
    private final Monitor monitor;

    public CommonsConnectionPoolConfigFactory(@NotNull ServiceExtensionContext serviceExtensionContext) {
        this.serviceExtensionContext = Objects.requireNonNull(serviceExtensionContext);
        this.monitor = serviceExtensionContext.getMonitor();
    }

    public CommonsConnectionPoolConfig create() {

        boolean invalidConfiguration = false;

        CommonsConnectionPoolConfig.Builder builder = CommonsConnectionPoolConfig.Builder.newInstance();

        // MAX IDLE CONNECTIONS
        try {
            Integer maxIdleConnections = getIntegerSetting(SettingKeys.POSTGRESQL_CONNECTION_POOL_MAX_IDLE_CONNECTIONS);
            if (maxIdleConnections != null) {
                builder.maxIdleConnections(maxIdleConnections);
            }
        } catch (NumberFormatException ignore) {
            monitor.severe(String.format(LOG_INVALID_NUMBER, SettingKeys.POSTGRESQL_CONNECTION_POOL_MAX_IDLE_CONNECTIONS));
            invalidConfiguration = true;
        }

        // MAX TOTAL CONNECTIONS
        try {
            Integer maxTotalConnections = getIntegerSetting(SettingKeys.POSTGRESQL_CONNECTION_POOL_MAX_TOTAL_CONNECTIONS);
            if (maxTotalConnections != null) {
                builder.maxTotalConnections(maxTotalConnections);
            }
        } catch (NumberFormatException ignore) {
            monitor.severe(String.format(LOG_INVALID_NUMBER, SettingKeys.POSTGRESQL_CONNECTION_POOL_MAX_TOTAL_CONNECTIONS));
            invalidConfiguration = true;
        }

        // MIN IDLE CONNECTIONS
        try {
            Integer minIdleConnections = getIntegerSetting(SettingKeys.POSTGRESQL_CONNECTION_POOL_MIN_IDLE_CONNECTIONS);
            if (minIdleConnections != null) {
                builder.minIdleConnections(minIdleConnections);
            }
        } catch (NumberFormatException ignore) {
            monitor.severe(String.format(LOG_INVALID_NUMBER, SettingKeys.POSTGRESQL_CONNECTION_POOL_MIN_IDLE_CONNECTIONS));
            invalidConfiguration = true;
        }

        // TEST CONNECTIONS ON BORROW
        Boolean testConnectionOnBorrow = getBooleanSetting(SettingKeys.POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_ON_BORROW);
        if (testConnectionOnBorrow != null) {
            builder.testConnectionOnBorrow(testConnectionOnBorrow);
        }

        // TEST CONNECTIONS ON CREATE
        Boolean testConnectionOnCreate = getBooleanSetting(SettingKeys.POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_ON_CREATE);
        if (testConnectionOnCreate != null) {
            builder.testConnectionOnCreate(testConnectionOnCreate);
        }

        // TEST CONNECTIONS ON RETURN
        Boolean testConnectionOnReturn = getBooleanSetting(SettingKeys.POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_ON_RETURN);
        if (testConnectionOnReturn != null) {
            builder.testConnectionOnReturn(testConnectionOnReturn);
        }

        // TEST CONNECTION WHILE IDLE
        Boolean testConnectionWhileIdle = getBooleanSetting(SettingKeys.POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_WHILE_IDLE);
        if (testConnectionWhileIdle != null) {
            builder.testConnectionWhileIdle(testConnectionWhileIdle);
        }

        // TEST QUERY
        String testQuery = getStringSetting(SettingKeys.POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_QUERY);
        if (testQuery != null) {
            builder.testQuery(testQuery);
        }

        if (invalidConfiguration) {
            throw new EdcException(EXCEPTION_INVALID_SETTINGS);
        } else {
            return builder.build();
        }
    }

    private Integer getIntegerSetting(String key) {
        String setting = serviceExtensionContext.getSetting(key, null);
        return setting == null ? null : Integer.parseInt(setting);
    }

    private Boolean getBooleanSetting(String key) {
        String setting = serviceExtensionContext.getSetting(key, null);
        return setting == null ? null : Boolean.parseBoolean(setting);
    }

    @SuppressWarnings("SameParameterValue")
    private String getStringSetting(String key) {
        return serviceExtensionContext.getSetting(key, null);
    }
}
