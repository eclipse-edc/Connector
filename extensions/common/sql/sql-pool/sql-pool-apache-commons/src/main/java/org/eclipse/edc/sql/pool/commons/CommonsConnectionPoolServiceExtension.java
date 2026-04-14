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
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.edc.sql.pool.commons;

import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.sql.ConnectionFactory;
import org.eclipse.edc.sql.datasource.ConnectionFactoryDataSource;
import org.eclipse.edc.sql.datasource.ConnectionPoolDataSource;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.sql.DataSource;

import static java.util.Optional.ofNullable;

@Extension(value = CommonsConnectionPoolServiceExtension.NAME)
public class CommonsConnectionPoolServiceExtension implements ServiceExtension {

    public static final String NAME = "Commons Connection Pool";

    public static final String EDC_DATASOURCE_PREFIX = "edc.datasource";
    public static final String URL = "url";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String POOL_CONNECTIONS_MAX_IDLE = "pool.connections.max-idle";
    public static final String POOL_CONNECTIONS_MAX_TOTAL = "pool.connections.max-total";
    public static final String POOL_CONNECTIONS_MIN_IDLE = "pool.connections.min-idle";
    public static final String POOL_CONNECTION_TEST_ON_BORROW = "pool.connection.test.on-borrow";
    public static final String POOL_CONNECTION_TEST_ON_CREATE = "pool.connection.test.on-create";
    public static final String POOL_CONNECTION_TEST_ON_RETURN = "pool.connection.test.on-return";
    public static final String POOL_CONNECTION_TEST_WHILE_IDLE = "pool.connection.test.while-idle";
    public static final String POOL_CONNECTION_TEST_QUERY = "pool.connection.test.query";

    @SettingContext(EDC_DATASOURCE_PREFIX)
    @Configuration
    private Map<String, DatasourceConfiguration> datasources;

    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private Monitor monitor;
    @Inject
    private ConnectionFactory connectionFactory;
    @Inject
    private Vault vault;

    private final List<CommonsConnectionPool> commonsConnectionPools = new LinkedList<>();

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        datasources.forEach((name, configuration) -> {
            var rootPath = EDC_DATASOURCE_PREFIX + "." + name;
            var dataSource = createDataSource(rootPath, configuration, context.getConfig(rootPath));
            var connectionPool = createConnectionPool(dataSource, configuration);
            commonsConnectionPools.add(connectionPool);
            var connectionPoolDataSource = new ConnectionPoolDataSource(connectionPool);
            dataSourceRegistry.register(name, connectionPoolDataSource);
        });
    }

    @Override
    public void cleanup() {
        commonsConnectionPools.forEach(CommonsConnectionPool::close);
    }

    private DataSource createDataSource(String rootPath, DatasourceConfiguration configuration, Config currentNode) {
        var jdbcUrl = getSecretOrSetting(rootPath, URL, configuration.url())
                .orElseThrow(() -> new EdcException("Mandatory url for datasource '%s' not found. Please provide a value for it, either as a secret in the vault or an application property.".formatted(currentNode.currentNode())));
        var jdbcUser = getSecretOrSetting(rootPath, USER, configuration.user());
        var jdbcPassword = getSecretOrSetting(rootPath, PASSWORD, configuration.password());

        var properties = new Properties();
        properties.putAll(currentNode.getRelativeEntries());

        jdbcUser.ifPresent(u -> properties.put(USER, u));
        jdbcPassword.ifPresent(p -> properties.put(PASSWORD, p));

        return new ConnectionFactoryDataSource(connectionFactory, jdbcUrl, properties);
    }

    private Optional<String> getSecretOrSetting(String rootPath, String key, String configValue) {
        var fullKey = rootPath + "." + key;
        return ofNullable(vault.resolveSecret(fullKey))
                .or(() -> {
                    monitor.warning("Datasource configuration value '%s' not found in vault, will fall back to Config. Please consider putting datasource configuration into the vault.".formatted(fullKey));
                    return Optional.ofNullable(configValue);
                });
    }

    private CommonsConnectionPool createConnectionPool(DataSource dataSource, DatasourceConfiguration configuration) {
        var builder = CommonsConnectionPoolConfig.Builder.newInstance();

        setIfProvided(configuration::poolConnectionsMaxIdle, builder::maxIdleConnections);
        setIfProvided(configuration::poolConnectionsMaxTotal, builder::maxTotalConnections);
        setIfProvided(configuration::poolConnectionsMinIdle, builder::minIdleConnections);
        setIfProvided(configuration::poolConnectionTestOnBorrow, builder::testConnectionOnBorrow);
        setIfProvided(configuration::poolConnectionTestOnCreate, builder::testConnectionOnCreate);
        setIfProvided(configuration::poolConnectionTestOnReturn, builder::testConnectionOnReturn);
        setIfProvided(configuration::poolConnectionTestWhileIdle, builder::testConnectionWhileIdle);
        setIfProvided(configuration::poolConnectionTestQuery, builder::testQuery);

        return new CommonsConnectionPool(dataSource, builder.build(), monitor);
    }

    private <T> void setIfProvided(Supplier<T> supplier, Consumer<T> setter) {
        var value = supplier.get();
        if (value != null) {
            setter.accept(value);
        }
    }

    @Settings
    private record DatasourceConfiguration(
            @Setting(
                    key = URL,
                    description = "JDBC url",
                    required = false)
            String url,
            @Setting(
                    key = USER,
                    description = "Username to be used for the JDBC connection. Can be omitted if not required, or if the user is encoded in the JDBC url.",
                    required = false)
            String user,
            @Setting(
                    key = PASSWORD,
                    description = "Username to be used for the JDBC connection. Can be omitted if not required, or if the password is encoded in the JDBC url.",
                    required = false)
            String password,

            @Setting(
                    key = POOL_CONNECTIONS_MAX_IDLE,
                    description = "Pool max idle connections",
                    required = false)
            Integer poolConnectionsMaxIdle,
            @Setting(
                    key = POOL_CONNECTIONS_MAX_TOTAL,
                    description = "Pool max total connections",
                    required = false)
            Integer poolConnectionsMaxTotal,
            @Setting(
                    key = POOL_CONNECTIONS_MIN_IDLE,
                    description = "Pool min idle connections",
                    required = false)
            Integer poolConnectionsMinIdle,
            @Setting(
                    key = POOL_CONNECTION_TEST_ON_BORROW,
                    description = "Pool test on borrow",
                    required = false)
            Boolean poolConnectionTestOnBorrow,
            @Setting(
                    key = POOL_CONNECTION_TEST_ON_CREATE,
                    description = "Pool test on create",
                    required = false)
            Boolean poolConnectionTestOnCreate,
            @Setting(
                    key = POOL_CONNECTION_TEST_ON_RETURN,
                    description = "Pool test on return",
                    required = false)
            Boolean poolConnectionTestOnReturn,
            @Setting(
                    key = POOL_CONNECTION_TEST_WHILE_IDLE,
                    description = "Pool test while idle",
                    required = false)
            Boolean poolConnectionTestWhileIdle,
            @Setting(
                    key = POOL_CONNECTION_TEST_QUERY,
                    description = "Pool test query",
                    required = false)
            String poolConnectionTestQuery
    ) {

    }
}
