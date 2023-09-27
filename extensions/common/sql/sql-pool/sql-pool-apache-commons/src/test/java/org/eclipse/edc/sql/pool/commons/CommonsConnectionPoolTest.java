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

package org.eclipse.edc.sql.pool.commons;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommonsConnectionPoolTest {

    private final Monitor monitor = mock();

    @Test
    void getConnection() throws SQLException {
        var connection = mock(Connection.class);
        var testQueryPreparedStatement = mock(PreparedStatement.class);
        var dataSource = mock(DataSource.class);
        var commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        var connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig, monitor);

        when(testQueryPreparedStatement.execute()).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(testQueryPreparedStatement);
        when(dataSource.getConnection()).thenReturn(connection);

        var result = connectionPool.getConnection();

        assertNotNull(connection);
        assertEquals(connection, result);

        verify(dataSource, atLeastOnce()).getConnection();
        verify(connection, atLeastOnce()).isClosed();
        verify(connection, atLeastOnce()).prepareStatement(anyString());
        verify(testQueryPreparedStatement, atLeastOnce()).execute();
    }

    @Test
    void getConnectionAnyExceptionThrownThrowsSqlException() throws SQLException {
        var dataSource = mock(DataSource.class);
        var commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        var connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig, monitor);
        var causingRuntimeException = new RuntimeException("intended to be thrown");

        when(dataSource.getConnection()).thenThrow(causingRuntimeException);

        var exceptionWrappingRuntimeException = assertThrows(EdcPersistenceException.class, connectionPool::getConnection);

        assertNotNull(exceptionWrappingRuntimeException.getCause());
        assertEquals(causingRuntimeException, exceptionWrappingRuntimeException.getCause());

        verify(dataSource, atLeastOnce()).getConnection();
    }

    @Test
    void getConnectionSqlExceptionThrownThrowsSame() throws SQLException {
        var dataSource = mock(DataSource.class);
        var commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        var connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig, monitor);
        var causingSqlException = new SQLException("intended to be thrown");

        when(dataSource.getConnection()).thenThrow(causingSqlException);

        var sqlException = assertThrows(EdcPersistenceException.class, connectionPool::getConnection);

        assertNotNull(sqlException.getCause());
        assertEquals(causingSqlException, sqlException.getCause());

        verify(dataSource, atLeastOnce()).getConnection();
    }

    @Test
    void returnConnectionNullThrowsNullPointerException() {
        var dataSource = mock(DataSource.class);
        var commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        var connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig, monitor);

        assertThrows(NullPointerException.class, () -> connectionPool.returnConnection(null));
    }

    @Test
    void returnConnectionUnknownThrowsIllegalStateException() {
        var dataSource = mock(DataSource.class);
        var commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        var connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig, monitor);

        // a connection unmanaged by the pool
        var connection = mock(Connection.class);

        assertThrows(IllegalStateException.class, () -> connectionPool.returnConnection(connection));
    }

    @Test
    void returnConnection() throws SQLException {
        var connection = mock(Connection.class);
        PreparedStatement testQueryPreparedStatement = mock(PreparedStatement.class);
        var dataSource = mock(DataSource.class);
        var commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        var connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig, monitor);

        when(testQueryPreparedStatement.execute()).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(testQueryPreparedStatement);
        when(dataSource.getConnection()).thenReturn(connection);

        var result = connectionPool.getConnection();

        assertNotNull(connection);
        assertEquals(connection, result);

        connectionPool.returnConnection(result);

        verify(dataSource, atLeastOnce()).getConnection();
        verify(connection, atLeastOnce()).isClosed();
        verify(connection, atLeastOnce()).prepareStatement(anyString());
        verify(testQueryPreparedStatement, atLeastOnce()).execute();
        verify(connection, atLeastOnce()).rollback();

    }
    
    @Test
    void returnConnection_shouldInvalidateConnection_rollbackFailure() throws SQLException {
        var connection = mock(Connection.class);
        var testQueryPreparedStatement = mock(PreparedStatement.class);
        when(testQueryPreparedStatement.execute()).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(testQueryPreparedStatement);
        var dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        var commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance()
                .testConnectionOnCreate(false)
                .testConnectionOnBorrow(false)
                .testConnectionOnReturn(true)
                .build();
        var connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig, monitor);

        var result = connectionPool.getConnection();

        assertNotNull(connection);
        assertEquals(connection, result);

        when(connection.isClosed()).thenReturn(false);

        doThrow(new SQLException()).when(connection).rollback();

        connectionPool.returnConnection(connection);

        verify(dataSource, atLeastOnce()).getConnection();
        verify(connection, atLeastOnce()).isClosed();
        verify(connection, atLeastOnce()).prepareStatement(anyString());
        verify(testQueryPreparedStatement, atLeastOnce()).execute();
        verify(connection, atLeastOnce()).close();

        verify(connection).rollback();

    }

    @Test
    void closeProperlyClosesManagedConnections() throws SQLException {
        var connection = mock(Connection.class);
        var testQueryPreparedStatement = mock(PreparedStatement.class);
        when(testQueryPreparedStatement.execute()).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(testQueryPreparedStatement);
        var dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        var commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();
        var connectionPool = new CommonsConnectionPool(dataSource, commonsConnectionPoolConfig, monitor);

        var result = connectionPool.getConnection();

        assertNotNull(connection);
        assertEquals(connection, result);

        connectionPool.returnConnection(connection);

        connectionPool.close();

        verify(dataSource, atLeastOnce()).getConnection();
        verify(connection, atLeastOnce()).isClosed();
        verify(connection, atLeastOnce()).prepareStatement(anyString());
        verify(testQueryPreparedStatement, atLeastOnce()).execute();
        verify(connection, atLeastOnce()).close();
    }
}
