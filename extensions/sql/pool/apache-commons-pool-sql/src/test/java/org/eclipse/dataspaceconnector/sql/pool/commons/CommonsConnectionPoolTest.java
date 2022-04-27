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

import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

class CommonsConnectionPoolTest {

    @Test
    void getConnection() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement testQueryPreparedStatement = Mockito.mock(PreparedStatement.class);
        DataSource dataSource = Mockito.mock(DataSource.class);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig);

        Mockito.when(testQueryPreparedStatement.execute()).thenReturn(true);
        Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(testQueryPreparedStatement);
        Mockito.when(dataSource.getConnection()).thenReturn(connection);

        Connection result = connectionPool.getConnection();

        Assertions.assertNotNull(connection);
        Assertions.assertEquals(connection, result);

        Mockito.verify(dataSource, Mockito.atLeastOnce()).getConnection();
        Mockito.verify(connection, Mockito.atLeastOnce()).isClosed();
        Mockito.verify(connection, Mockito.atLeastOnce()).prepareStatement(Mockito.anyString());
        Mockito.verify(testQueryPreparedStatement, Mockito.atLeastOnce()).execute();
    }

    @Test
    void getConnectionAnyExceptionThrownThrowsSqlException() throws SQLException {
        DataSource dataSource = Mockito.mock(DataSource.class);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig);
        RuntimeException causingRuntimeException = new RuntimeException("intended to be thrown");

        Mockito.when(dataSource.getConnection()).thenThrow(causingRuntimeException);

        EdcPersistenceException exceptionWrappingRuntimeException = Assertions.assertThrows(EdcPersistenceException.class, connectionPool::getConnection);

        Assertions.assertNotNull(exceptionWrappingRuntimeException.getCause());
        Assertions.assertEquals(causingRuntimeException, exceptionWrappingRuntimeException.getCause());

        Mockito.verify(dataSource, Mockito.atLeastOnce()).getConnection();
    }

    @Test
    void getConnectionSqlExceptionThrownThrowsSame() throws SQLException {
        DataSource dataSource = Mockito.mock(DataSource.class);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig);
        SQLException causingSqlException = new SQLException("intended to be thrown");

        Mockito.when(dataSource.getConnection()).thenThrow(causingSqlException);

        EdcPersistenceException sqlException = Assertions.assertThrows(EdcPersistenceException.class, connectionPool::getConnection);

        Assertions.assertNotNull(sqlException.getCause());
        Assertions.assertEquals(causingSqlException, sqlException.getCause());

        Mockito.verify(dataSource, Mockito.atLeastOnce()).getConnection();
    }

    @Test
    void returnConnectionNullThrowsNullPointerException() {
        DataSource dataSource = Mockito.mock(DataSource.class);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig);

        Assertions.assertThrows(NullPointerException.class, () -> connectionPool.returnConnection(null));
    }

    @Test
    void returnConnectionUnknownThrowsIllegalStateException() {
        DataSource dataSource = Mockito.mock(DataSource.class);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig);

        // a connection unmanaged by the pool
        Connection connection = Mockito.mock(Connection.class);

        Assertions.assertThrows(IllegalStateException.class, () -> connectionPool.returnConnection(connection));
    }

    @Test
    void returnConnection() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement testQueryPreparedStatement = Mockito.mock(PreparedStatement.class);
        DataSource dataSource = Mockito.mock(DataSource.class);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig);

        Mockito.when(testQueryPreparedStatement.execute()).thenReturn(true);
        Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(testQueryPreparedStatement);
        Mockito.when(dataSource.getConnection()).thenReturn(connection);

        Connection result = connectionPool.getConnection();

        Assertions.assertNotNull(connection);
        Assertions.assertEquals(connection, result);

        connectionPool.returnConnection(result);

        Mockito.verify(dataSource, Mockito.atLeastOnce()).getConnection();
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
        DataSource dataSource = Mockito.mock(DataSource.class);
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance()
                .testConnectionOnCreate(false)
                .testConnectionOnBorrow(false)
                .build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig);

        Connection result = connectionPool.getConnection();

        Assertions.assertNotNull(connection);
        Assertions.assertEquals(connection, result);

        Mockito.when(connection.isClosed()).thenReturn(false);

        connectionPool.returnConnection(connection);

        Mockito.verify(dataSource, Mockito.atLeastOnce()).getConnection();
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
        DataSource dataSource = Mockito.mock(DataSource.class);
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        CommonsConnectionPool connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig);

        Connection result = connectionPool.getConnection();

        Assertions.assertNotNull(connection);
        Assertions.assertEquals(connection, result);

        connectionPool.returnConnection(connection);

        connectionPool.close();

        Mockito.verify(dataSource, Mockito.atLeastOnce()).getConnection();
        Mockito.verify(connection, Mockito.atLeastOnce()).isClosed();
        Mockito.verify(connection, Mockito.atLeastOnce()).prepareStatement(Mockito.anyString());
        Mockito.verify(testQueryPreparedStatement, Mockito.atLeastOnce()).execute();
        Mockito.verify(connection, Mockito.atLeastOnce()).close();
    }
}
