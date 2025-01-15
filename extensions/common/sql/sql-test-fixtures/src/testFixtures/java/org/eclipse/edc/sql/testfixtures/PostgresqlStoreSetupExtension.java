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

import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
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
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static java.lang.String.format;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * Extension for running PG SQL store implementation. It automatically creates a test database and provided all the base data structure
 * for a SQL store to run such as {@link DataSourceRegistry}, {@link TransactionContext} and data source name which is automatically generated
 */
public class PostgresqlStoreSetupExtension implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback, ParameterResolver {

    public static final String POSTGRES_IMAGE_NAME = "postgres:16.1";
    private final PostgreSQLContainer<?> postgreSqlContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE_NAME)
            .withExposedPorts(5432)
            .withUsername("postgres")
            .withPassword("password")
            .withDatabaseName("itest");
    private final PostgresqlLocalInstance postgres;
    private final QueryExecutor queryExecutor = new SqlQueryExecutor();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final DataSourceRegistry dataSourceRegistry = new DefaultDataSourceRegistry();
    private final String datasourceName = UUID.randomUUID().toString();
    private final String jdbcUrlPrefix;

    public PostgresqlStoreSetupExtension() {
        var exposedPort = getFreePort();
        postgreSqlContainer.setPortBindings(List.of("%s:5432".formatted(exposedPort)));
        jdbcUrlPrefix = format("jdbc:postgresql://%s:%s/", postgreSqlContainer.getHost(), exposedPort);
        postgres = new PostgresqlLocalInstance(postgreSqlContainer.getUsername(), postgreSqlContainer.getPassword(), jdbcUrlPrefix);
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

    @Override
    public void beforeAll(ExtensionContext context) {
        postgreSqlContainer.start();
        postgres.createDatabase(postgreSqlContainer.getDatabaseName());
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var properties = new Properties();
        properties.put("user", postgreSqlContainer.getUsername());
        properties.put("password", postgreSqlContainer.getPassword());
        var connectionFactory = new DriverManagerConnectionFactory();
        var jdbcUrl = jdbcUrlPrefix + postgreSqlContainer.getDatabaseName();
        var dataSource = new ConnectionFactoryDataSource(connectionFactory, jdbcUrl, properties);
        dataSourceRegistry.register(datasourceName, dataSource);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        postgreSqlContainer.stop();
        postgreSqlContainer.close();
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
            return getConnection();
        } else if (type.equals(QueryExecutor.class)) {
            return queryExecutor;
        } else if (type.equals(PostgresqlLocalInstance.class)) {
            return postgres;
        }
        return null;
    }

    public Config getDatasourceConfig() {
        return ConfigFactory.fromMap(getDatasourceConfiguration());
    }

    public Map<String, String> getDatasourceConfiguration() {
        return postgres.createDefaultDatasourceConfiguration(postgreSqlContainer.getDatabaseName());
    }
}
