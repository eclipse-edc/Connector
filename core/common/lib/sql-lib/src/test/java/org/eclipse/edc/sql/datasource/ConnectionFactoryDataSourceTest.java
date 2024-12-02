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

import org.eclipse.edc.sql.ConnectionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConnectionFactoryDataSourceTest {

    private final ConnectionFactory connectionFactory = mock();
    private final Connection connection = mock();
    private final ConnectionFactoryDataSource connectionFactoryDataSource = new ConnectionFactoryDataSource(
            connectionFactory, "jdbcUrl", new Properties());

    @Test
    void getConnection() throws SQLException {
        when(connectionFactory.create(any(), any())).thenReturn(connection);

        var result = connectionFactoryDataSource.getConnection();

        assertThat(result).isSameAs(connection);
        verify(connectionFactory, times(1)).create(any(), any());
    }

    @Test
    void getConnectionWithArgument() throws SQLException {
        when(connectionFactory.create(any(), any())).thenReturn(connection);

        var result = connectionFactoryDataSource.getConnection(null, null);

        assertThat(result).isSameAs(connection);
        verify(connectionFactory, times(1)).create(any(), any());
    }

    @Test
    void getLogWriter() {
        assertThrows(SQLFeatureNotSupportedException.class, connectionFactoryDataSource::getLogWriter);

        verifyNoInteractions(connectionFactory);
    }

    @Test
    void setLogWriter() {
        assertThrows(SQLFeatureNotSupportedException.class, () -> connectionFactoryDataSource.setLogWriter(mock()));

        verifyNoInteractions(connectionFactory);
    }

    @Test
    void setLoginTimeout() {
        assertThrows(SQLFeatureNotSupportedException.class, () -> connectionFactoryDataSource.setLoginTimeout(1));

        verifyNoInteractions(connectionFactory);
    }

    @Test
    void getLoginTimeout() {
        assertThrows(SQLFeatureNotSupportedException.class, connectionFactoryDataSource::getLoginTimeout);

        verifyNoInteractions(connectionFactory);
    }

    @Test
    void getParentLogger() {
        assertThrows(SQLFeatureNotSupportedException.class, connectionFactoryDataSource::getParentLogger);

        verifyNoInteractions(connectionFactory);
    }

    @Test
    void unwrap() {
        assertThrows(SQLFeatureNotSupportedException.class, () -> connectionFactoryDataSource.unwrap(Class.class));

        verifyNoInteractions(connectionFactory);
    }

    @Test
    void isWrapperFor() throws SQLException {
        var result = connectionFactoryDataSource.isWrapperFor(Class.class);

        Assertions.assertFalse(result);

        verifyNoInteractions(connectionFactory);
    }

    @Test
    void createConnectionBuilder() {
        assertThrows(SQLFeatureNotSupportedException.class, connectionFactoryDataSource::createConnectionBuilder);
        verifyNoInteractions(connectionFactory);
    }
}
