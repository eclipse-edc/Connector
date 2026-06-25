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

package org.eclipse.edc.sql.datasource;

import org.eclipse.edc.sql.ConnectionFactory;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * A DataSource utilizing a {@link ConnectionFactory}.
 */
public class ConnectionFactoryDataSource implements DataSource {

    private final ConnectionFactory connectionFactory;
    private final String jdbcUrl;
    private final Properties properties;

    public ConnectionFactoryDataSource(ConnectionFactory connectionFactory, String jdbcUrl, Properties properties) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
        this.jdbcUrl = jdbcUrl;
        this.properties = properties;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connectionFactory.create(jdbcUrl, properties);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return connectionFactory.create(jdbcUrl, properties);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLogWriter(PrintWriter printWriter) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
