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
 *       Daimler TSS GmbH - Initial Test
 *
 */

package org.eclipse.edc.sql.datasource;

import org.eclipse.edc.sql.pool.ConnectionPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

class ConnectionPoolDataSourceTest {

    @Test
    void constructor() {
        Assertions.assertThrows(NullPointerException.class, () -> new ConnectionPoolDataSource(null));

        ConnectionPool connectionPool = Mockito.mock(ConnectionPool.class);

        new ConnectionPoolDataSource(connectionPool);

        Mockito.verifyNoInteractions(connectionPool);
    }

    @Test
    void getConnection() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        ConnectionPool connectionPool = Mockito.mock(ConnectionPool.class);
        Mockito.when(connectionPool.getConnection()).thenReturn(connection);

        ConnectionPoolDataSource connectionPoolDataSource = new ConnectionPoolDataSource(connectionPool);

        Connection result = connectionPoolDataSource.getConnection();

        Assertions.assertNotNull(result);

        Mockito.verify(connectionPool, Mockito.times(1)).getConnection();

        result.close();

        Mockito.verify(connectionPool, Mockito.times(1)).returnConnection(connection);
    }

    @Test
    void getConnectionWithArgument() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        ConnectionPool connectionPool = Mockito.mock(ConnectionPool.class);
        Mockito.when(connectionPool.getConnection()).thenReturn(connection);

        ConnectionPoolDataSource connectionPoolDataSource = new ConnectionPoolDataSource(connectionPool);

        Connection result = connectionPoolDataSource.getConnection(null, null);

        Assertions.assertNotNull(result);

        Mockito.verify(connectionPool, Mockito.times(1)).getConnection();

        result.close();

        Mockito.verify(connectionPool, Mockito.times(1)).returnConnection(connection);
    }

    @Test
    void getLogWriter() {
        ConnectionPool connectionPool = Mockito.mock(ConnectionPool.class);

        ConnectionPoolDataSource connectionPoolDataSource = new ConnectionPoolDataSource(connectionPool);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, connectionPoolDataSource::getLogWriter);

        Mockito.verifyNoInteractions(connectionPool);
    }

    @Test
    void setLogWriter() {
        PrintWriter printWriter = Mockito.mock(PrintWriter.class);

        ConnectionPool connectionPool = Mockito.mock(ConnectionPool.class);

        ConnectionPoolDataSource connectionPoolDataSource = new ConnectionPoolDataSource(connectionPool);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> connectionPoolDataSource.setLogWriter(printWriter));

        Mockito.verifyNoInteractions(connectionPool);
    }

    @Test
    void setLoginTimeout() {
        ConnectionPool connectionPool = Mockito.mock(ConnectionPool.class);

        ConnectionPoolDataSource connectionPoolDataSource = new ConnectionPoolDataSource(connectionPool);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> connectionPoolDataSource.setLoginTimeout(1));

        Mockito.verifyNoInteractions(connectionPool);
    }

    @Test
    void getLoginTimeout() {
        ConnectionPool connectionPool = Mockito.mock(ConnectionPool.class);

        ConnectionPoolDataSource connectionPoolDataSource = new ConnectionPoolDataSource(connectionPool);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, connectionPoolDataSource::getLoginTimeout);

        Mockito.verifyNoInteractions(connectionPool);
    }

    @Test
    void getParentLogger() {
        ConnectionPool connectionPool = Mockito.mock(ConnectionPool.class);

        ConnectionPoolDataSource connectionPoolDataSource = new ConnectionPoolDataSource(connectionPool);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, connectionPoolDataSource::getParentLogger);

        Mockito.verifyNoInteractions(connectionPool);
    }

    @Test
    void unwrap() {
        ConnectionPool connectionPool = Mockito.mock(ConnectionPool.class);

        ConnectionPoolDataSource connectionPoolDataSource = new ConnectionPoolDataSource(connectionPool);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> connectionPoolDataSource.unwrap(Class.class));

        Mockito.verifyNoInteractions(connectionPool);
    }

    @Test
    void isWrapperFor() throws SQLException {
        ConnectionPool connectionPool = Mockito.mock(ConnectionPool.class);

        ConnectionPoolDataSource connectionPoolDataSource = new ConnectionPoolDataSource(connectionPool);

        boolean result = connectionPoolDataSource.isWrapperFor(Class.class);

        Assertions.assertFalse(result);

        Mockito.verifyNoInteractions(connectionPool);
    }

    @Test
    void createConnectionBuilder() {
        ConnectionPool connectionPool = Mockito.mock(ConnectionPool.class);

        ConnectionPoolDataSource connectionPoolDataSource = new ConnectionPoolDataSource(connectionPool);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, connectionPoolDataSource::createConnectionBuilder);

        Mockito.verifyNoInteractions(connectionPool);
    }
}
