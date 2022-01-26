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

package org.eclipse.dataspaceconnector.sql.postgresql;

import org.eclipse.dataspaceconnector.sql.ConnectionFactory;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * A ConnectionFactory implementation that utilizes the {@code ConnectionFactoryConfig}
 * for creating connections towards postgres databases.
 */
public class PostgresqlConnectionFactory implements ConnectionFactory {
    private final String jdbcUrl;
    private final Properties properties;
    private final boolean autoCommit;

    /**
     * Constructs a new PostgresqlConnectionFactory for obtaining {@code java.sql.Connection}s
     * directed towards a configured postgres database.
     *
     * @param postgresqlConnectionFactoryConfig containing connection parameters required for obtaining {@code java.sql.Connection}s
     */
    public PostgresqlConnectionFactory(@NotNull PostgresqlConnectionFactoryConfig postgresqlConnectionFactoryConfig) {
        Objects.requireNonNull(postgresqlConnectionFactoryConfig, "connectionFactoryConfig");

        this.jdbcUrl = Objects.requireNonNull(postgresqlConnectionFactoryConfig.getUri()).toString();
        this.properties = getProperties(postgresqlConnectionFactoryConfig);
        this.autoCommit = postgresqlConnectionFactoryConfig.getAutoCommit();
    }

    /**
     * Constructs a map containing postgreSQL jdbc connection parameters to obtain a {@code java.sql.Connection}.
     *
     * @param postgresqlConnectionFactoryConfig container object containing set of connection configuration parameters
     * @return properties to be used by the {@link DriverManager}
     * @see <a href="https://jdbc.postgresql.org/documentation/head/connect.html">PostgreSQL JDBC Driver - Connecting to the Database</a>
     */
    private Properties getProperties(PostgresqlConnectionFactoryConfig postgresqlConnectionFactoryConfig) {
        Properties properties = new Properties();
        addProperty(properties, "user", postgresqlConnectionFactoryConfig.getUserName());
        addProperty(properties, "password", postgresqlConnectionFactoryConfig.getPassword());
        addProperty(properties, "ssl", postgresqlConnectionFactoryConfig.getSsl());
        addProperty(properties, "sslmode", postgresqlConnectionFactoryConfig.getSslMode());
        addProperty(properties, "sslcert", postgresqlConnectionFactoryConfig.getSslCert());
        addProperty(properties, "sslkey", postgresqlConnectionFactoryConfig.getSslKey());
        addProperty(properties, "sslrootcert", postgresqlConnectionFactoryConfig.getSslRootCert());
        addProperty(properties, "sslhostnameverifier", postgresqlConnectionFactoryConfig.getSslHostNameVerifier());
        addProperty(properties, "loggerLevel", postgresqlConnectionFactoryConfig.getLoggerLevel());
        addProperty(properties, "loggerFile", postgresqlConnectionFactoryConfig.getLoggerFile());
        addProperty(properties, "logUnclosedConnections", postgresqlConnectionFactoryConfig.getLogUnclosedConnection());
        addProperty(properties, "connectTimeout", postgresqlConnectionFactoryConfig.getConnectTimeout());
        addProperty(properties, "socketTimeout", postgresqlConnectionFactoryConfig.getSocketTimeout());
        addProperty(properties, "loginTimeout", postgresqlConnectionFactoryConfig.getLoginTimeout());
        addProperty(properties, "readOnly", postgresqlConnectionFactoryConfig.getReadOnly());
        addProperty(properties, "ApplicationName", postgresqlConnectionFactoryConfig.getApplicationName());
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

        return connection;
    }
}
