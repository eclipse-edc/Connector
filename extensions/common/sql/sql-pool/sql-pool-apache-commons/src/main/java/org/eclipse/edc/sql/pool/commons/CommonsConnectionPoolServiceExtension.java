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
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.sql.ConnectionFactory;
import org.eclipse.edc.sql.datasource.ConnectionFactoryDataSource;
import org.eclipse.edc.sql.datasource.ConnectionPoolDataSource;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javax.sql.DataSource;

import static java.lang.String.format;

@Extension(value = CommonsConnectionPoolServiceExtension.NAME)
public class CommonsConnectionPoolServiceExtension implements ServiceExtension {

    public static final String NAME = "Commons Connection Pool";

    public static final String EDC_DATASOURCE_PREFIX = "edc.datasource";
    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    public static final String DEPRACATED_POOL_MAX_IDLE_CONNECTIONS = "pool.maxIdleConnections";
    public static final String POOL_CONNECTIONS_MAX_IDLE = "pool.connections.max-idle";
    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    public static final String DEPRACATED_POOL_MAX_TOTAL_CONNECTIONS = "pool.maxTotalConnections";
    public static final String POOL_CONNECTIONS_MAX_TOTAL = "pool.connections.max-total";
    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    public static final String DEPRACATED_POOL_MIN_IDLE_CONNECTIONS = "pool.minIdleConnections";
    public static final String POOL_CONNECTIONS_MIN_IDLE = "pool.connections.min-idle";
    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    public static final String DEPRACATED_POOL_TEST_CONNECTION_ON_BORROW = "pool.testConnectionOnBorrow";
    public static final String POOL_CONNECTION_TEST_ON_BORROW = "pool.connection.test.on-borrow";
    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    public static final String DEPRACATED_POOL_TEST_CONNECTION_ON_CREATE = "pool.testConnectionOnCreate";
    public static final String POOL_CONNECTION_TEST_ON_CREATE = "pool.connection.test.on-create";
    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    public static final String DEPRACATED_POOL_TEST_CONNECTION_ON_RETURN = "pool.testConnectionOnReturn";
    public static final String POOL_CONNECTION_TEST_ON_RETURN = "pool.connection.test.on-return";
    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    public static final String DEPRACATED_POOL_TEST_CONNECTION_WHILE_IDLE = "pool.testConnectionWhileIdle";
    public static final String POOL_CONNECTION_TEST_WHILE_IDLE = "pool.connection.test.while-idle";
    @Deprecated(since = "0.3.1")
    @Setting(required = false)
    public static final String DEPRACATED_POOL_TEST_QUERY = "pool.testQuery";
    public static final String POOL_CONNECTION_TEST_QUERY = "pool.connection.test.query";
    public static final Map<String, String> CONFIGURATION_MAPPING = Map.of(
            POOL_CONNECTIONS_MAX_IDLE, DEPRACATED_POOL_MAX_IDLE_CONNECTIONS,
            POOL_CONNECTIONS_MIN_IDLE, DEPRACATED_POOL_MIN_IDLE_CONNECTIONS,
            POOL_CONNECTIONS_MAX_TOTAL, DEPRACATED_POOL_MAX_TOTAL_CONNECTIONS,
            POOL_CONNECTION_TEST_ON_BORROW, DEPRACATED_POOL_TEST_CONNECTION_ON_BORROW,
            POOL_CONNECTION_TEST_ON_CREATE, DEPRACATED_POOL_TEST_CONNECTION_ON_CREATE,
            POOL_CONNECTION_TEST_ON_RETURN, DEPRACATED_POOL_TEST_CONNECTION_ON_RETURN,
            POOL_CONNECTION_TEST_WHILE_IDLE, DEPRACATED_POOL_TEST_CONNECTION_WHILE_IDLE,
            POOL_CONNECTION_TEST_QUERY, DEPRACATED_POOL_TEST_QUERY);

    @Setting(required = true)
    public static final String URL = "url";
    private final List<CommonsConnectionPool> commonsConnectionPools = new LinkedList<>();
    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private Monitor monitor;

    @Inject
    private ConnectionFactory connectionFactory;

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
        var oldKey = CONFIGURATION_MAPPING.get(key);
        var oldValue = getter.apply(oldKey, null);
        if (oldValue != null) {
            monitor.warning(format("Configuration setting %s has been deprecated, please use %s instead", oldKey, key));
        }
        var value = getter.apply(key, oldValue);
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
        var jdbcUrl = Objects.requireNonNull(config.getString(URL));

        var properties = new Properties();
        properties.putAll(config.getRelativeEntries());

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
