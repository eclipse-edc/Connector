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

package org.eclipse.dataspaceconnector.sql.pool.commons;

import org.eclipse.dataspaceconnector.sql.connection.ConnectionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class CommonsConnectionPoolTest {

    @Test
    void getConnection() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement testQueryPreparedStatement = Mockito.mock(PreparedStatement.class);
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(connectionFactory, commonsConnectionPoolConfig);

        Mockito.when(testQueryPreparedStatement.execute()).thenReturn(true);
        Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(testQueryPreparedStatement);
        Mockito.when(connectionFactory.create()).thenReturn(connection);

        Connection result = connectionPool.getConnection();

        Assertions.assertNotNull(connection);
        Assertions.assertEquals(connection, result);

        Mockito.verify(connectionFactory, Mockito.atLeastOnce()).create();
        Mockito.verify(connection, Mockito.atLeastOnce()).isClosed();
        Mockito.verify(connection, Mockito.atLeastOnce()).prepareStatement(Mockito.anyString());
        Mockito.verify(testQueryPreparedStatement, Mockito.atLeastOnce()).execute();
    }

    @Test
    void getConnectionAnyExceptionThrownThrowsSqlException() throws SQLException {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(connectionFactory, commonsConnectionPoolConfig);
        RuntimeException causingRuntimeException = new RuntimeException("intended to be thrown");

        Mockito.when(connectionFactory.create()).thenThrow(causingRuntimeException);

        SQLException exceptionWrappingRuntimeException = Assertions.assertThrows(SQLException.class, connectionPool::getConnection);

        Assertions.assertNotNull(exceptionWrappingRuntimeException.getCause());
        Assertions.assertEquals(causingRuntimeException, exceptionWrappingRuntimeException.getCause());

        Mockito.verify(connectionFactory, Mockito.atLeastOnce()).create();
    }

    @Test
    void getConnectionSqlExceptionThrownThrowsSame() throws SQLException {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(connectionFactory, commonsConnectionPoolConfig);
        SQLException causingSqlException = new SQLException("intended to be thrown");

        Mockito.when(connectionFactory.create()).thenThrow(causingSqlException);

        SQLException sqlException = Assertions.assertThrows(SQLException.class, connectionPool::getConnection);

        Assertions.assertNull(sqlException.getCause());
        Assertions.assertEquals(causingSqlException, sqlException);

        Mockito.verify(connectionFactory, Mockito.atLeastOnce()).create();
    }

    @Test
    void returnConnectionNullThrowsNullPointerException() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(connectionFactory, commonsConnectionPoolConfig);

        Assertions.assertThrows(NullPointerException.class, () -> connectionPool.returnConnection(null));
    }

    @Test
    void returnConnectionUnknownThrowsIllegalStateException() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(connectionFactory, commonsConnectionPoolConfig);

        // a connection unmanaged by the pool
        Connection connection = Mockito.mock(Connection.class);

        Assertions.assertThrows(IllegalStateException.class, () -> connectionPool.returnConnection(connection));
    }

    @Test
    void returnConnection() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement testQueryPreparedStatement = Mockito.mock(PreparedStatement.class);
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(connectionFactory, commonsConnectionPoolConfig);

        Mockito.when(testQueryPreparedStatement.execute()).thenReturn(true);
        Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(testQueryPreparedStatement);
        Mockito.when(connectionFactory.create()).thenReturn(connection);

        Connection result = connectionPool.getConnection();

        Assertions.assertNotNull(connection);
        Assertions.assertEquals(connection, result);

        connectionPool.returnConnection(result);

        Mockito.verify(connectionFactory, Mockito.atLeastOnce()).create();
        Mockito.verify(connection, Mockito.atLeastOnce()).isClosed();
        Mockito.verify(connection, Mockito.atLeastOnce()).prepareStatement(Mockito.anyString());
        Mockito.verify(testQueryPreparedStatement, Mockito.atLeastOnce()).execute();
    }

    @Test
    void returnConnectionProperlyClosed() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement testQueryPreparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(testQueryPreparedStatement.execute()).thenReturn(false);
        Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(testQueryPreparedStatement);
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        Mockito.when(connectionFactory.create()).thenReturn(connection);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance()
                .testConnectionOnCreate(false)
                .testConnectionOnBorrow(false)
                .build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(connectionFactory, commonsConnectionPoolConfig);

        Connection result = connectionPool.getConnection();

        Assertions.assertNotNull(connection);
        Assertions.assertEquals(connection, result);

        Mockito.when(connection.isClosed()).thenReturn(false);

        connectionPool.returnConnection(connection);

        Mockito.verify(connectionFactory, Mockito.atLeastOnce()).create();
        Mockito.verify(connection, Mockito.atLeastOnce()).isClosed();
        Mockito.verify(connection, Mockito.atLeastOnce()).prepareStatement(Mockito.anyString());
        Mockito.verify(testQueryPreparedStatement, Mockito.atLeastOnce()).execute();
        Mockito.verify(connection, Mockito.atLeastOnce()).close();
    }

    @Test
    void closeProperlyClosesManagedConnections() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement testQueryPreparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(testQueryPreparedStatement.execute()).thenReturn(true);
        Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(testQueryPreparedStatement);
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        Mockito.when(connectionFactory.create()).thenReturn(connection);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(connectionFactory, commonsConnectionPoolConfig);

        Connection result = connectionPool.getConnection();

        Assertions.assertNotNull(connection);
        Assertions.assertEquals(connection, result);

        connectionPool.returnConnection(connection);

        connectionPool.close();

        Mockito.verify(connectionFactory, Mockito.atLeastOnce()).create();
        Mockito.verify(connection, Mockito.atLeastOnce()).isClosed();
        Mockito.verify(connection, Mockito.atLeastOnce()).prepareStatement(Mockito.anyString());
        Mockito.verify(testQueryPreparedStatement, Mockito.atLeastOnce()).execute();
        Mockito.verify(connection, Mockito.atLeastOnce()).close();
    }
}