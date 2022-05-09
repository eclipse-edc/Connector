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

package org.eclipse.dataspaceconnector.sql.datasource;

import org.eclipse.dataspaceconnector.sql.ConnectionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

class ConnectionFactoryDataSourceTest {

    @Test
    void constructor() {
        Assertions.assertThrows(NullPointerException.class, () -> new ConnectionFactoryDataSource(null));

        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);

        new ConnectionFactoryDataSource(connectionFactory);

        Mockito.verifyNoInteractions(connectionFactory);
    }

    @Test
    void getConnection() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        Mockito.when(connectionFactory.create()).thenReturn(connection);

        ConnectionFactoryDataSource connectionFactoryDataSource = new ConnectionFactoryDataSource(connectionFactory);

        Connection result = connectionFactoryDataSource.getConnection();

        Assertions.assertEquals(connection, result);

        Mockito.verify(connectionFactory, Mockito.times(1)).create();
    }

    @Test
    void getConnectionWithArgument() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        Mockito.when(connectionFactory.create()).thenReturn(connection);

        ConnectionFactoryDataSource connectionFactoryDataSource = new ConnectionFactoryDataSource(connectionFactory);

        Connection result = connectionFactoryDataSource.getConnection(null, null);

        Assertions.assertEquals(connection, result);

        Mockito.verify(connectionFactory, Mockito.times(1)).create();
    }

    @Test
    void getLogWriter() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);

        ConnectionFactoryDataSource connectionFactoryDataSource = new ConnectionFactoryDataSource(connectionFactory);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, connectionFactoryDataSource::getLogWriter);

        Mockito.verifyNoInteractions(connectionFactory);
    }

    @Test
    void setLogWriter() {
        PrintWriter printWriter = Mockito.mock(PrintWriter.class);

        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);

        ConnectionFactoryDataSource connectionFactoryDataSource = new ConnectionFactoryDataSource(connectionFactory);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> connectionFactoryDataSource.setLogWriter(printWriter));

        Mockito.verifyNoInteractions(connectionFactory);
    }

    @Test
    void setLoginTimeout() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);

        ConnectionFactoryDataSource connectionFactoryDataSource = new ConnectionFactoryDataSource(connectionFactory);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> connectionFactoryDataSource.setLoginTimeout(1));

        Mockito.verifyNoInteractions(connectionFactory);
    }

    @Test
    void getLoginTimeout() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);

        ConnectionFactoryDataSource connectionFactoryDataSource = new ConnectionFactoryDataSource(connectionFactory);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, connectionFactoryDataSource::getLoginTimeout);

        Mockito.verifyNoInteractions(connectionFactory);
    }

    @Test
    void getParentLogger() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);

        ConnectionFactoryDataSource connectionFactoryDataSource = new ConnectionFactoryDataSource(connectionFactory);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, connectionFactoryDataSource::getParentLogger);

        Mockito.verifyNoInteractions(connectionFactory);
    }

    @Test
    void unwrap() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);

        ConnectionFactoryDataSource connectionFactoryDataSource = new ConnectionFactoryDataSource(connectionFactory);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, () -> connectionFactoryDataSource.unwrap(Class.class));

        Mockito.verifyNoInteractions(connectionFactory);
    }

    @Test
    void isWrapperFor() throws SQLException {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);

        ConnectionFactoryDataSource connectionFactoryDataSource = new ConnectionFactoryDataSource(connectionFactory);

        boolean result = connectionFactoryDataSource.isWrapperFor(Class.class);

        Assertions.assertFalse(result);

        Mockito.verifyNoInteractions(connectionFactory);
    }

    @Test
    void createConnectionBuilder() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);

        ConnectionFactoryDataSource connectionFactoryDataSource = new ConnectionFactoryDataSource(connectionFactory);

        Assertions.assertThrows(SQLFeatureNotSupportedException.class, connectionFactoryDataSource::createConnectionBuilder);

        Mockito.verifyNoInteractions(connectionFactory);
    }
}
