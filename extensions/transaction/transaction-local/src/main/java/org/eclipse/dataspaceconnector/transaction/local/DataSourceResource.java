/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transaction.local;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.transaction.local.LocalTransactionResource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Wraps a DataSource so that it can be enlisted in a local transaction context.
 */
public class DataSourceResource implements LocalTransactionResource, DataSource {
    private ThreadLocal<ConnectionWrapper> enlistedConnections = new ThreadLocal<>();

    private DataSource delegate;

    public DataSourceResource(DataSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void commit() {
        try {
            var connection = enlistedConnections.get();
            if (connection == null) {
                // no resource used, ignore
                return;
            }
            try {
                connection.getWrappedConnection().commit();
            } finally {
                connection.getWrappedConnection().close();
            }
        } catch (SQLException e) {
            throw new EdcException(e);
        } finally {
            enlistedConnections.remove();
        }
    }

    @Override
    public void rollback() {
        try {
            var connection = enlistedConnections.get();
            if (connection == null) {
                // no resource used, ignore
                return;
            }
            try {
                connection.getWrappedConnection().rollback();
            } finally {
                connection.getWrappedConnection().close();
            }
        } catch (SQLException e) {
            throw new EdcException(e);
        } finally {
            enlistedConnections.remove();
        }
    }

    @Override
    public Connection getConnection() {
        try {
            var connection = enlistedConnections.get();
            if (connection == null) {
                var delegate = this.delegate.getConnection();
                delegate.setAutoCommit(false);
                connection = new ConnectionWrapper(delegate);
                enlistedConnections.set(connection);
            }
            return connection;
        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public Connection getConnection(String username, String password) {
        try {
            var connection = enlistedConnections.get();
            if (connection == null) {
                connection = new ConnectionWrapper(delegate.getConnection(username, password));
                enlistedConnections.set(connection);
            }
            return connection;
        } catch (SQLException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }


}
