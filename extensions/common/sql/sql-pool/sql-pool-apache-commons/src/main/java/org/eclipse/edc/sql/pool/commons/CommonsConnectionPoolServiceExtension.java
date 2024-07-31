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

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
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
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javax.sql.DataSource;

import static java.util.Optional.ofNullable;

@Extension(value = CommonsConnectionPoolServiceExtension.NAME)
public class CommonsConnectionPoolServiceExtension implements ServiceExtension {

    public static final String NAME = "Commons Connection Pool";

    public static final String EDC_DATASOURCE_PREFIX = "edc.datasource";
    private static final String EDC_DATASOURCE_CONFIG_CONTEXT = EDC_DATASOURCE_PREFIX + ".<name>";

    @Setting(value = "JDBC url", required = true, context = EDC_DATASOURCE_CONFIG_CONTEXT)
    public static final String URL = "url";
    @Setting(value = "Username to be used for the JDBC connection. Can be omitted if not required, or if the user is encoded in the JDBC url.", context = EDC_DATASOURCE_CONFIG_CONTEXT)
    public static final String USER = "user";
    @Setting(value = "Username to be used for the JDBC connection. Can be omitted if not required, or if the password is encoded in the JDBC url.", context = EDC_DATASOURCE_CONFIG_CONTEXT)
    public static final String PASSWORD = "password";

    @Setting(value = "Pool max idle connections", type = "int", context = EDC_DATASOURCE_CONFIG_CONTEXT)
    public static final String POOL_CONNECTIONS_MAX_IDLE = "pool.connections.max-idle";
    @Setting(value = "Pool max total connections", type = "int", context = EDC_DATASOURCE_CONFIG_CONTEXT)
    public static final String POOL_CONNECTIONS_MAX_TOTAL = "pool.connections.max-total";
    @Setting(value = "Pool min idle connections", type = "int", context = EDC_DATASOURCE_CONFIG_CONTEXT)
    public static final String POOL_CONNECTIONS_MIN_IDLE = "pool.connections.min-idle";
    @Setting(value = "Pool test on borrow", type = "boolean", context = EDC_DATASOURCE_CONFIG_CONTEXT)
    public static final String POOL_CONNECTION_TEST_ON_BORROW = "pool.connection.test.on-borrow";
    @Setting(value = "Pool test on create", type = "boolean", context = EDC_DATASOURCE_CONFIG_CONTEXT)
    public static final String POOL_CONNECTION_TEST_ON_CREATE = "pool.connection.test.on-create";
    @Setting(value = "Pool test on return", type = "boolean", context = EDC_DATASOURCE_CONFIG_CONTEXT)
    public static final String POOL_CONNECTION_TEST_ON_RETURN = "pool.connection.test.on-return";
    @Setting(value = "Pool test while idle", type = "boolean", context = EDC_DATASOURCE_CONFIG_CONTEXT)
    public static final String POOL_CONNECTION_TEST_WHILE_IDLE = "pool.connection.test.while-idle";
    @Setting(value = "Pool test query", context = EDC_DATASOURCE_CONFIG_CONTEXT)
    public static final String POOL_CONNECTION_TEST_QUERY = "pool.connection.test.query";

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
        context.getConfig(EDC_DATASOURCE_PREFIX).partition().forEach(config -> {
            var dataSourceName = config.currentNode();
            var dataSource = createDataSource(config);
            var connectionPool = createConnectionPool(dataSource, config);
            commonsConnectionPools.add(connectionPool);
            var connectionPoolDataSource = new ConnectionPoolDataSource(connectionPool);
            dataSourceRegistry.register(dataSourceName, connectionPoolDataSource);
        });
    }

    @Override
    public void shutdown() {
        commonsConnectionPools.forEach(CommonsConnectionPool::close);
    }

    private DataSource createDataSource(Config config) {
        var rootPath = EDC_DATASOURCE_PREFIX + "." + config.currentNode();

        var jdbcUrl = getSecretOrSetting(rootPath, URL, config)
                .orElseThrow(() -> new EdcException("Mandatory url for datasource '%s' not found. Please provide a value for it, either as a secret in the vault or an application property.".formatted(config.currentNode())));
        var jdbcUser = getSecretOrSetting(rootPath, USER, config);
        var jdbcPassword = getSecretOrSetting(rootPath, PASSWORD, config);

        var properties = new Properties();
        properties.putAll(config.getRelativeEntries());

        jdbcUser.ifPresent(u -> properties.put(USER, u));
        jdbcPassword.ifPresent(p -> properties.put(PASSWORD, p));

        return new ConnectionFactoryDataSource(connectionFactory, jdbcUrl, properties);
    }

    private Optional<String> getSecretOrSetting(String rootPath, String key, Config config) {
        var fullKey = rootPath + "." + key;
        return ofNullable(vault.resolveSecret(fullKey))
                .or(() -> {
                    monitor.warning("Datasource configuration value '%s' not found in vault, will fall back to Config. Please consider putting datasource configuration into the vault.".formatted(fullKey));
                    return Optional.ofNullable(config.getString(key, null));
                });
    }

    private CommonsConnectionPool createConnectionPool(DataSource unPooledDataSource, Config config) {
        var builder = CommonsConnectionPoolConfig.Builder.newInstance();

        setIfProvided(POOL_CONNECTIONS_MAX_IDLE, config::getInteger, builder::maxIdleConnections);
        setIfProvided(POOL_CONNECTIONS_MAX_TOTAL, config::getInteger, builder::maxTotalConnections);
        setIfProvided(POOL_CONNECTIONS_MIN_IDLE, config::getInteger, builder::minIdleConnections);
        setIfProvided(POOL_CONNECTION_TEST_ON_BORROW, config::getBoolean, builder::testConnectionOnBorrow);
        setIfProvided(POOL_CONNECTION_TEST_ON_CREATE, config::getBoolean, builder::testConnectionOnCreate);
        setIfProvided(POOL_CONNECTION_TEST_ON_RETURN, config::getBoolean, builder::testConnectionOnReturn);
        setIfProvided(POOL_CONNECTION_TEST_WHILE_IDLE, config::getBoolean, builder::testConnectionWhileIdle);
        setIfProvided(POOL_CONNECTION_TEST_QUERY, config::getString, builder::testQuery);

        return new CommonsConnectionPool(unPooledDataSource, builder.build(), monitor);
    }

    private <T> void setIfProvided(String key, BiFunction<String, T, T> getter, Consumer<T> setter) {
        var value = getter.apply(key, null);
        if (value != null) {
            setter.accept(value);
        }
    }
}
