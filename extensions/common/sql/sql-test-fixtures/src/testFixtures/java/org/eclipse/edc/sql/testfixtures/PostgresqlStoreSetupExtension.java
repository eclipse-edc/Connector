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

import org.eclipse.edc.sql.DriverManagerConnectionFactory;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.sql.datasource.ConnectionFactoryDataSource;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * Extension for running PostgreSQL store implementation tests. It starts a database container and provides all the base
 * data structure for a SQL store to run such as {@link DataSourceRegistry}, {@link TransactionContext} and data source
 * name which is automatically generated.
 */
public class PostgresqlStoreSetupExtension implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final String DEFAULT_IMAGE = "postgres:17.3";

    private final PostgreSQLContainer<?> postgres;
    private final QueryExecutor queryExecutor = new SqlQueryExecutor();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final DataSourceRegistry dataSourceRegistry = new DefaultDataSourceRegistry();
    private final String datasourceName = UUID.randomUUID().toString();

    public PostgresqlStoreSetupExtension() {
        this(DEFAULT_IMAGE);
    }

    public PostgresqlStoreSetupExtension(String dockerImageName) {
        postgres = new PostgreSQLContainer<>(dockerImageName);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        postgres.start();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var properties = new Properties();
        properties.put("user", postgres.getUsername());
        properties.put("password", postgres.getPassword());
        var connectionFactory = new DriverManagerConnectionFactory();
        var jdbcUrl = postgres.getJdbcUrl() + postgres.getDatabaseName();
        var dataSource = new ConnectionFactoryDataSource(connectionFactory, jdbcUrl, properties);
        dataSourceRegistry.register(datasourceName, dataSource);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        postgres.stop();
        postgres.close();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        return List.of(PostgresqlStoreSetupExtension.class, Connection.class, QueryExecutor.class).contains(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(PostgresqlStoreSetupExtension.class)) {
            return this;
        } else if (type.equals(Connection.class)) {
            return getConnection();
        } else if (type.equals(QueryExecutor.class)) {
            return queryExecutor;
        }
        return null;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public Connection getConnection() {
        try {
            return dataSourceRegistry.resolve(datasourceName).getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int runQuery(String query) {
        return transactionContext.execute(() -> queryExecutor.execute(getConnection(), query));
    }

    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    public DataSourceRegistry getDataSourceRegistry() {
        return dataSourceRegistry;
    }

}
