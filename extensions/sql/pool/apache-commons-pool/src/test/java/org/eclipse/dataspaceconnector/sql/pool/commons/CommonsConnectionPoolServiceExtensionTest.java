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
 *       Daimler TSS GmbH - Initial Extension Test
 *
 */

package org.eclipse.dataspaceconnector.sql.pool.commons;

import org.eclipse.dataspaceconnector.transaction.local.DataSourceResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.IntStream;
import javax.sql.DataSource;

class CommonsConnectionPoolServiceExtensionTest extends AbstractCommonsConnectionPoolServiceExtensionTest {
    private static final String SQL_QUERY = "SELECT 1";
    private static final String DS_1_NAME = "ds1";
    private static final String DS_2_NAME = "ds2";

    private final Map<String, String> systemProperties = new HashMap<>() {
        {
            put("edc.datasource." + DS_1_NAME + ".url", DS_1_NAME);
            put("edc.datasource." + DS_1_NAME + "." + CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_ON_CREATE, "false");
            put("edc.datasource." + DS_1_NAME + "." + CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_ON_RETURN, "false");
            put("edc.datasource." + DS_1_NAME + "." + CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_ON_BORROW, "false");
            put("edc.datasource." + DS_1_NAME + "." + CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_WHILE_IDLE, "false");
            put("edc.datasource." + DS_2_NAME + ".url", DS_2_NAME);
            put("edc.datasource." + DS_2_NAME + "." + CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_ON_CREATE, "false");
            put("edc.datasource." + DS_2_NAME + "." + CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_ON_RETURN, "false");
            put("edc.datasource." + DS_2_NAME + "." + CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_ON_BORROW, "false");
            put("edc.datasource." + DS_2_NAME + "." + CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_WHILE_IDLE, "false");
        }
    };

    // mocks
    private Connection dataSource1Connection;
    private Connection dataSource2Connection;

    @BeforeEach
    void setUp() {
        dataSource1Connection = Mockito.mock(Connection.class);
        dataSource2Connection = Mockito.mock(Connection.class);

        systemProperties.forEach(System::setProperty);
    }

    @AfterEach
    void tearDown() {
        systemProperties.keySet().forEach(System::clearProperty);
    }

    @Test
    @DisplayName("DataSource Registry contains defined DataSources")
    void testDataSourceRegistryContainsDataSources() {
        DataSource dataSource = getDataSourceRegistry().resolve(DS_1_NAME);

        Assertions.assertNotNull(dataSource);
        Assertions.assertInstanceOf(DataSourceResource.class, dataSource);

        dataSource = getDataSourceRegistry().resolve(DS_2_NAME);
        Assertions.assertNotNull(dataSource);
        Assertions.assertInstanceOf(DataSourceResource.class, dataSource);
    }

    @Test
    @DisplayName("Used DataSource is scoped by the TransactionContext")
    void testUsedDataSourceIsScopedByTransactionContext() throws SQLException {
        PreparedStatement preparedStatementMock = Mockito.mock(PreparedStatement.class);

        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(Mockito.eq(DS_1_NAME), Mockito.any(Properties.class))).thenReturn(dataSource1Connection);

            Mockito.when(dataSource1Connection.prepareStatement(SQL_QUERY)).thenReturn(preparedStatementMock);

            getTransactionContext().execute(() -> {
                DataSource dataSource = getDataSourceRegistry().resolve(DS_1_NAME);
                try (Connection connection = dataSource.getConnection()) {
                    try (PreparedStatement statement = connection.prepareStatement(SQL_QUERY)) {
                        statement.execute();
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });

            driverManagerMock.verify(() -> DriverManager.getConnection(Mockito.eq(DS_1_NAME), Mockito.any(Properties.class)), Mockito.times(1));
            driverManagerMock.verify(() -> DriverManager.getConnection(Mockito.eq(DS_2_NAME), Mockito.any(Properties.class)), Mockito.never());
        }

        Mockito.verify(dataSource1Connection, Mockito.times(1)).setAutoCommit(false);

        Mockito.verify(preparedStatementMock, Mockito.times(1)).execute();

        Mockito.verify(preparedStatementMock, Mockito.times(1)).close();

        Mockito.verify(dataSource1Connection, Mockito.times(1)).commit();
    }

    @Test
    @DisplayName("DataSourcePool reuses issued Connection")
    void testDataSourcePoolReusesConnections() throws SQLException {
        PreparedStatement preparedStatementMock = Mockito.mock(PreparedStatement.class);

        final int iterations = 3;
        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(Mockito.eq(DS_1_NAME), Mockito.any(Properties.class))).thenReturn(dataSource1Connection);

            Mockito.when(dataSource1Connection.prepareStatement(SQL_QUERY)).thenReturn(preparedStatementMock);

            IntStream.rangeClosed(1, iterations).forEach((iteration) -> {
                getTransactionContext().execute(() -> {
                    DataSource dataSource = getDataSourceRegistry().resolve(DS_1_NAME);
                    try (Connection connection = dataSource.getConnection()) {
                        try (PreparedStatement statement = connection.prepareStatement(SQL_QUERY)) {
                            statement.execute();
                        }
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                });
            });

            driverManagerMock.verify(() -> DriverManager.getConnection(Mockito.eq(DS_1_NAME), Mockito.any(Properties.class)), Mockito.times(1));
            driverManagerMock.verify(() -> DriverManager.getConnection(Mockito.eq(DS_2_NAME), Mockito.any(Properties.class)), Mockito.never());
        }

        Mockito.verify(dataSource1Connection, Mockito.times(iterations)).setAutoCommit(false);

        Mockito.verify(preparedStatementMock, Mockito.times(iterations)).execute();

        Mockito.verify(preparedStatementMock, Mockito.times(iterations)).close();

        Mockito.verify(dataSource1Connection, Mockito.times(iterations)).commit();
    }

    @Test
    @DisplayName("All used DataSources are scoped by the TransactionContext")
    void testAllDataSourceAreScopedByTransactionContext() throws SQLException {
        PreparedStatement preparedStatementMock1 = Mockito.mock(PreparedStatement.class);
        PreparedStatement preparedStatementMock2 = Mockito.mock(PreparedStatement.class);

        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(Mockito.eq(DS_1_NAME), Mockito.any(Properties.class))).thenReturn(dataSource1Connection);
            driverManagerMock.when(() -> DriverManager.getConnection(Mockito.eq(DS_2_NAME), Mockito.any(Properties.class))).thenReturn(dataSource2Connection);

            Mockito.when(dataSource1Connection.prepareStatement(SQL_QUERY)).thenReturn(preparedStatementMock1);
            Mockito.when(dataSource2Connection.prepareStatement(SQL_QUERY)).thenReturn(preparedStatementMock2);

            getTransactionContext().execute(() -> {
                DataSource dataSource1 = getDataSourceRegistry().resolve(DS_1_NAME);
                try (Connection connection = dataSource1.getConnection()) {
                    try (PreparedStatement statement = connection.prepareStatement(SQL_QUERY)) {
                        statement.execute();
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
                DataSource dataSource2 = getDataSourceRegistry().resolve(DS_2_NAME);
                try (Connection connection = dataSource2.getConnection()) {
                    try (PreparedStatement statement = connection.prepareStatement(SQL_QUERY)) {
                        statement.execute();
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });

            driverManagerMock.verify(() -> DriverManager.getConnection(Mockito.eq(DS_1_NAME), Mockito.any(Properties.class)), Mockito.times(1));
            driverManagerMock.verify(() -> DriverManager.getConnection(Mockito.eq(DS_2_NAME), Mockito.any(Properties.class)), Mockito.times(1));
        }

        Mockito.verify(dataSource1Connection, Mockito.times(1)).setAutoCommit(false);
        Mockito.verify(dataSource2Connection, Mockito.times(1)).setAutoCommit(false);

        Mockito.verify(preparedStatementMock1, Mockito.times(1)).execute();
        Mockito.verify(preparedStatementMock1, Mockito.times(1)).close();
        Mockito.verify(preparedStatementMock2, Mockito.times(1)).execute();
        Mockito.verify(preparedStatementMock2, Mockito.times(1)).close();

        Mockito.verify(dataSource1Connection, Mockito.times(1)).commit();
        Mockito.verify(dataSource2Connection, Mockito.times(1)).commit();
    }
}
