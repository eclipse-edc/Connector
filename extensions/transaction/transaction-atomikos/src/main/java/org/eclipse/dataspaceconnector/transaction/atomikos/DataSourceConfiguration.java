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

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Configuration for an Atomikos datasource.
 */
public class DataSourceConfiguration {
    public enum DataSourceType {
        XA, NON_XA
    }

    private String name;
    private String driverClass;
    private DataSourceType dataSourceType = DataSourceType.XA;
    private String url;
    private String username;
    private String password;
    private int maxPoolSize = -1;
    private int minPoolSize = -1;
    private int connectionTimeout = -1;
    private int loginTimeout = -1;
    private int maintenanceInterval = -1;
    private int maxIdle = -1;
    private int poolSize = -1;
    private int reap = -1;
    private String query;

    private Properties properties = new Properties();

    public DataSourceConfiguration(String name, String driverClass, DataSourceType dataSourceType) {
        this.name = name;
        this.driverClass = driverClass;
        this.dataSourceType = dataSourceType;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getDriverClass() {
        return driverClass;
    }

    @NotNull
    public DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    @NotNull
    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getLoginTimeout() {
        return loginTimeout;
    }

    public int getMaintenanceInterval() {
        return maintenanceInterval;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getReap() {
        return reap;
    }

    public String getQuery() {
        return query;
    }

    /**
     * Returns the CONFIGURED driver-specific properties.
     */
    public Properties getProperties() {
        return properties;
    }

    private DataSourceConfiguration() {
    }

    public static class Builder {
        private DataSourceConfiguration configuration;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder name(String name) {
            configuration.name = name;
            return this;
        }

        public Builder driverClass(String driverClass) {
            configuration.driverClass = driverClass;
            return this;
        }

        public Builder dataSourceType(DataSourceType dataSourceType) {
            configuration.dataSourceType = dataSourceType;
            return this;
        }

        public Builder url(String url) {
            configuration.url = url;
            return this;
        }

        public Builder username(String username) {
            configuration.username = username;
            return this;
        }

        public Builder password(String password) {
            configuration.password = password;
            return this;
        }

        public Builder minPoolSize(int minPoolSize) {
            configuration.minPoolSize = minPoolSize;
            return this;
        }

        public Builder connectionTimeout(int connectionTimeout) {
            configuration.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder loginTimeout(int loginTimeout) {
            configuration.loginTimeout = loginTimeout;
            return this;
        }

        public Builder maintenanceInterval(int maintenanceInterval) {
            configuration.maintenanceInterval = maintenanceInterval;
            return this;
        }

        public Builder maxIdle(int maxIdle) {
            configuration.maxIdle = maxIdle;
            return this;
        }

        public Builder poolSize(int poolSize) {
            configuration.poolSize = poolSize;
            return this;
        }

        public Builder reap(int reap) {
            configuration.reap = reap;
            return this;
        }

        public Builder maxPoolSize(int maxPoolSize) {
            configuration.maxPoolSize = maxPoolSize;
            return this;
        }

        public Builder query(String query) {
            configuration.query = query;
            return this;
        }

        public Builder property(String key, String value) {
            configuration.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            configuration.properties.putAll(properties);
            return this;
        }

        public DataSourceConfiguration build() {
            Objects.requireNonNull(configuration.driverClass, "driverClass");
            Objects.requireNonNull(configuration.url, "url");
            return configuration;
        }

        private Builder() {
            configuration = new DataSourceConfiguration();
        }
    }
}
