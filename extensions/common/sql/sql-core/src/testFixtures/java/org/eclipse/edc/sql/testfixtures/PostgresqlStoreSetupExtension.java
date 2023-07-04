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

import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

import static java.lang.String.format;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Extension for running PG SQL store implementation. It automatically creates a test database and provided all the base data structure
 * for a SQL store to run such as {@link DataSourceRegistry}, {@link TransactionContext} and data source name which is automatically generated
 */
public class PostgresqlStoreSetupExtension implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback, ParameterResolver {

    public static final String POSTGRES_IMAGE_NAME = "postgres:14.2";
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE_NAME)
            .withExposedPorts(5432)
            .withUsername("postgres")
            .withPassword("password")
            .withDatabaseName("itest");
    private final String datasourceName;
    private final QueryExecutor queryExecutor = new SqlQueryExecutor();
    private DataSourceRegistry dataSourceRegistry = null;
    private DataSource dataSource = null;
    private Connection connection = null;
    private TransactionContext transactionContext = null;
    private PostgresqlLocalInstance helper;

    @SuppressWarnings("unused")
    public PostgresqlStoreSetupExtension() {
        this(UUID.randomUUID().toString());
    }

    public PostgresqlStoreSetupExtension(String datasourceName) {
        this.datasourceName = datasourceName;
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
        return transactionContext.execute(() -> queryExecutor.execute(connection, query));
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
        connection = spy(helper.getTestConnection(postgreSQLContainer.getHost(), postgreSQLContainer.getFirstMappedPort(), postgreSQLContainer.getDatabaseName()));

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
    public void beforeAll(ExtensionContext context) {
        postgreSQLContainer.start();
        var jdbcUrlPrefix = format("jdbc:postgresql://%s:%s/", postgreSQLContainer.getHost(), postgreSQLContainer.getFirstMappedPort());
        helper = new PostgresqlLocalInstance(postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword(), jdbcUrlPrefix, postgreSQLContainer.getDatabaseName());
        helper.createDatabase();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        postgreSQLContainer.stop();
        postgreSQLContainer.close();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        return List.of(PostgresqlStoreSetupExtension.class, Connection.class, QueryExecutor.class, PostgresqlLocalInstance.class).contains(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(PostgresqlStoreSetupExtension.class)) {
            return this;
        } else if (type.equals(Connection.class)) {
            return connection;
        } else if (type.equals(QueryExecutor.class)) {
            return queryExecutor;
        } else if (type.equals(PostgresqlLocalInstance.class)) {
            return helper;
        }
        return null;
    }
}
