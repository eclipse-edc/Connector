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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.sql.ConnectionFactory;
import org.eclipse.edc.sql.datasource.ConnectionFactoryDataSource;
import org.eclipse.edc.sql.datasource.ConnectionPoolDataSource;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.sql.DataSource;

import static java.util.Optional.ofNullable;

@Extension(value = CommonsConnectionPoolServiceExtension.NAME)
public class CommonsConnectionPoolServiceExtension implements ServiceExtension {

    public static final String NAME = "Commons Connection Pool";

    public static final String EDC_DATASOURCE_PREFIX = "edc.datasource";
    public static final String POOL_CONNECTIONS_MAX_IDLE = "pool.connections.max-idle";
    public static final String POOL_CONNECTIONS_MAX_TOTAL = "pool.connections.max-total";
    public static final String POOL_CONNECTIONS_MIN_IDLE = "pool.connections.min-idle";
    public static final String POOL_CONNECTION_TEST_ON_BORROW = "pool.connection.test.on-borrow";
    public static final String POOL_CONNECTION_TEST_ON_CREATE = "pool.connection.test.on-create";
    public static final String POOL_CONNECTION_TEST_ON_RETURN = "pool.connection.test.on-return";
    public static final String POOL_CONNECTION_TEST_WHILE_IDLE = "pool.connection.test.while-idle";
    public static final String POOL_CONNECTION_TEST_QUERY = "pool.connection.test.query";

    @Setting(required = true)
    public static final String URL = "url";

    @Setting(value = "Username to be used for the JDBC connection. Can be omitted if not required, or if the user is encoded in the JDBC url.")
    public static final String USER = "user";
    @Setting(value = "Username to be used for the JDBC connection. Can be omitted if not required, or if the password is encoded in the JDBC url.")
    public static final String PASSWORD = "password";

    private final List<CommonsConnectionPool> commonsConnectionPools = new LinkedList<>();
    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private Monitor monitor;

    @Inject
    private ConnectionFactory connectionFactory;

    @Inject
    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var config = context.getConfig(EDC_DATASOURCE_PREFIX);

        var namedConnectionPools = createConnectionPools(config);

        for (var entry : namedConnectionPools.entrySet()) {
            var dataSourceName = entry.getKey();
            var commonsConnectionPool = entry.getValue();
            commonsConnectionPools.add(commonsConnectionPool);
            var connectionPoolDataSource = new ConnectionPoolDataSource(commonsConnectionPool);
            dataSourceRegistry.register(dataSourceName, connectionPoolDataSource);
        }
    }

    @Override
    public void shutdown() {
        commonsConnectionPools.forEach(CommonsConnectionPool::close);
    }

    public List<CommonsConnectionPool> getCommonsConnectionPools() {
        return commonsConnectionPools;
    }

    private @NotNull Supplier<@Nullable String> readFromConfig(Config config, String value) {
        return () -> {
            monitor.warning("Database configuration value '%s' not found in vault, will fall back to Config. Please consider putting database configuration into the vault.");
            return config.getString(value, null);
        };
    }

    private void setIfProvidedString(String key, Consumer<String> setter, Config config) {
        setIfProvided(key, setter, config::getString);
    }

    private void setIfProvidedBoolean(String key, Consumer<Boolean> setter, Config config) {
        setIfProvided(key, setter, config::getBoolean);
    }

    private void setIfProvidedInt(String key, Consumer<Integer> setter, Config config) {
        setIfProvided(key, setter, config::getInteger);
    }

    private <T> void setIfProvided(String key, Consumer<T> setter, BiFunction<String, T, T> getter) {
        var value = getter.apply(key, null);
        if (value != null) {
            setter.accept(value);
        }
    }

    private Map<String, CommonsConnectionPool> createConnectionPools(Config parent) {
        Map<String, CommonsConnectionPool> commonsConnectionPools = new HashMap<>();
        for (var config : parent.partition().toList()) {
            var dataSourceName = config.currentNode();

            var dataSource = createDataSource(config);

            var commonsConnectionPool = createConnectionPool(dataSource, config);
            commonsConnectionPools.put(dataSourceName, commonsConnectionPool);
        }
        return commonsConnectionPools;
    }

    private DataSource createDataSource(Config config) {
        var rootPath = EDC_DATASOURCE_PREFIX + "." + config.currentNode();

        // read values from the vault first, fall back to config
        var jdbcUrl = Objects.requireNonNull(ofNullable(vault.resolveSecret(rootPath + "." + URL)).orElseGet(readFromConfig(config, URL)));
        var jdbcUser = ofNullable(vault.resolveSecret(rootPath + "." + USER))
                .orElseGet(readFromConfig(config, USER));
        var jdbcPassword = ofNullable(vault.resolveSecret(rootPath + "." + PASSWORD))
                .orElseGet(readFromConfig(config, PASSWORD));

        var properties = new Properties();
        properties.putAll(config.getRelativeEntries());

        // only set if not-null, otherwise Properties#add throws a NPE
        ofNullable(jdbcUser).ifPresent(u -> properties.put(USER, u));
        ofNullable(jdbcPassword).ifPresent(p -> properties.put(PASSWORD, p));

        return new ConnectionFactoryDataSource(connectionFactory, jdbcUrl, properties);
    }

    private CommonsConnectionPool createConnectionPool(DataSource unPooledDataSource, Config config) {
        var builder = CommonsConnectionPoolConfig.Builder.newInstance();

        setIfProvidedInt(POOL_CONNECTIONS_MAX_IDLE, builder::maxIdleConnections, config);
        setIfProvidedInt(POOL_CONNECTIONS_MAX_TOTAL, builder::maxTotalConnections, config);
        setIfProvidedInt(POOL_CONNECTIONS_MIN_IDLE, builder::minIdleConnections, config);
        setIfProvidedBoolean(POOL_CONNECTION_TEST_ON_BORROW, builder::testConnectionOnBorrow, config);
        setIfProvidedBoolean(POOL_CONNECTION_TEST_ON_CREATE, builder::testConnectionOnCreate, config);
        setIfProvidedBoolean(POOL_CONNECTION_TEST_ON_RETURN, builder::testConnectionOnReturn, config);
        setIfProvidedBoolean(POOL_CONNECTION_TEST_WHILE_IDLE, builder::testConnectionWhileIdle, config);
        setIfProvidedString(POOL_CONNECTION_TEST_QUERY, builder::testQuery, config);

        return new CommonsConnectionPool(unPooledDataSource, builder.build(), monitor);
    }
}
