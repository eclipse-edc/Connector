/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.sql.testfixtures;

import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.sql.Connection;
import java.util.UUID;
import javax.sql.DataSource;

import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuery;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Extension for running PG SQL store implementation. It automatically creates a test database and provided all the base data structure
 * for a SQL store to run such as {@link DataSourceRegistry}, {@link TransactionContext} and data source name which is automatically generated
 */
public class PostgresqlStoreSetupExtension implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, ParameterResolver {


    private final String datasourceName;
    private DataSourceRegistry dataSourceRegistry = null;
    private DataSource dataSource = null;
    private Connection connection = null;
    private TransactionContext transactionContext = null;

    public PostgresqlStoreSetupExtension(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public PostgresqlStoreSetupExtension() {
        this(UUID.randomUUID().toString());
    }


    public DataSource getDataSource() {
        return dataSource;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public Connection getConnection() {
        return connection;
    }

    public int runQuery(String query) {
        return transactionContext.execute(() -> executeQuery(connection, query));
    }


    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    public DataSourceRegistry getDataSourceRegistry() {
        return dataSourceRegistry;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        transactionContext = new NoopTransactionContext();
        dataSourceRegistry = mock(DataSourceRegistry.class);
        dataSource = mock(DataSource.class);
        connection = spy(PostgresqlLocalInstance.getTestConnection());

        when(dataSourceRegistry.resolve(datasourceName)).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        doNothing().when(connection).close();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        doCallRealMethod().when(connection).close();
        connection.close();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        PostgresqlLocalInstance.createTestDatabase();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        return type.equals(PostgresqlStoreSetupExtension.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws
            ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(PostgresqlStoreSetupExtension.class)) {
            return this;
        }
        return null;
    }
}
