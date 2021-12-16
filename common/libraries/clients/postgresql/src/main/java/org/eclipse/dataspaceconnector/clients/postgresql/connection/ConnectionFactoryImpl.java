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

package org.eclipse.dataspaceconnector.clients.postgresql.connection;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * A ConnectionFactory implementation that utilizes the ConnectionFactoryConfig for connection creation.
 */
public class ConnectionFactoryImpl implements ConnectionFactory {
    private final String jdbcUrl;
    private final Properties properties;
    private final boolean autoCommit;

    public ConnectionFactoryImpl(@NotNull ConnectionFactoryConfig connectionFactoryConfig) {
        Objects.requireNonNull(connectionFactoryConfig, "connectionFactoryConfig");

        this.jdbcUrl = Objects.requireNonNull(connectionFactoryConfig.getUri()).toString();
        this.properties = getProperties(connectionFactoryConfig);
        this.autoCommit = connectionFactoryConfig.getAutoCommit();
    }

    /**
     * Constructs a map containing postgreSQL jdbc connection parameters to obtain a connection.
     *
     * @param connectionFactoryConfig container object containing set of connection configuration parameters
     * @return properties to be used by the {@link java.sql.DriverManager}
     * @see <a href="https://jdbc.postgresql.org/documentation/head/connect.html">PostgreSQL JDBC Driver - Connecting to the Database</a>
     */
    private Properties getProperties(ConnectionFactoryConfig connectionFactoryConfig) {
        Properties properties = new Properties();
        addProperty(properties, "user", connectionFactoryConfig.getUserName());
        addProperty(properties, "password", connectionFactoryConfig.getPassword());
        addProperty(properties, "ssl", connectionFactoryConfig.getSsl());
        addProperty(properties, "sslmode", connectionFactoryConfig.getSslMode());
        addProperty(properties, "sslcert", connectionFactoryConfig.getSslCert());
        addProperty(properties, "sslkey", connectionFactoryConfig.getSslKey());
        addProperty(properties, "sslrootcert", connectionFactoryConfig.getSslRootCert());
        addProperty(properties, "sslhostnameverifier", connectionFactoryConfig.getSslHostNameVerifier());
        addProperty(properties, "loggerLevel", connectionFactoryConfig.getLoggerLevel());
        addProperty(properties, "loggerFile", connectionFactoryConfig.getLoggerFile());
        addProperty(properties, "logUnclosedConnections", connectionFactoryConfig.getLogUnclosedConnection());
        addProperty(properties, "connectTimeout", connectionFactoryConfig.getConnectTimeout());
        addProperty(properties, "socketTimeout", connectionFactoryConfig.getSocketTimeout());
        addProperty(properties, "loginTimeout", connectionFactoryConfig.getLoginTimeout());
        addProperty(properties, "readOnly", connectionFactoryConfig.getReadOnly());
        addProperty(properties, "ApplicationName", connectionFactoryConfig.getApplicationName());
        return properties;
    }

    private void addProperty(Properties properties, String key, Object value) {
        if (properties != null && key != null && value != null) {
            properties.put(key, String.valueOf(value));
        }
    }

    @Override
    public Connection create() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl, properties);

        connection.setAutoCommit(autoCommit);
        // TODO transaction isolation made configurable
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

        return connection;
    }
}
