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
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolConfigKeys.CONFIGURATION_MAPPING;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolConfigKeys.POOL_CONNECTIONS_MAX_IDLE;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolConfigKeys.POOL_CONNECTIONS_MAX_TOTAL;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolConfigKeys.POOL_CONNECTIONS_MIN_IDLE;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolConfigKeys.POOL_CONNECTION_TEST_ON_BORROW;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolConfigKeys.POOL_CONNECTION_TEST_ON_CREATE;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolConfigKeys.POOL_CONNECTION_TEST_ON_RETURN;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolConfigKeys.POOL_CONNECTION_TEST_QUERY;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolConfigKeys.POOL_CONNECTION_TEST_WHILE_IDLE;
import static org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolConfigKeys.URL;

@Extension(value = CommonsConnectionPoolServiceExtension.NAME)
public class CommonsConnectionPoolServiceExtension implements ServiceExtension {
    public static final String NAME = "Commons Connection Pool";
    public static final String EDC_DATASOURCE_PREFIX = "edc.datasource";
    private final List<CommonsConnectionPool> commonsConnectionPools = new LinkedList<>();
    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        Config config = context.getConfig(EDC_DATASOURCE_PREFIX);

        Map<String, CommonsConnectionPool> namedConnectionPools = createConnectionPools(config);

        for (Map.Entry<String, CommonsConnectionPool> entry : namedConnectionPools.entrySet()) {
            String dataSourceName = entry.getKey();
            CommonsConnectionPool commonsConnectionPool = entry.getValue();
            commonsConnectionPools.add(commonsConnectionPool);
            ConnectionPoolDataSource connectionPoolDataSource = new ConnectionPoolDataSource(commonsConnectionPool);
            dataSourceRegistry.register(dataSourceName, connectionPoolDataSource);
        }
    }

    @Override
    public void shutdown() {
        for (CommonsConnectionPool commonsConnectionPool : commonsConnectionPools) {
            commonsConnectionPool.close();
        }
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
        for (Config config : parent.partition().toList()) {
            String dataSourceName = config.currentNode();

            DataSource dataSource = createDataSource(config);

            CommonsConnectionPool commonsConnectionPool = createConnectionPool(dataSource, config);
            commonsConnectionPools.put(dataSourceName, commonsConnectionPool);
        }
        return commonsConnectionPools;
    }

    private DataSource createDataSource(Config config) {
        String jdbcUrl = Objects.requireNonNull(config.getString(URL));

        Properties properties = new Properties();
        properties.putAll(config.getRelativeEntries());

        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(jdbcUrl, properties);

        return new ConnectionFactoryDataSource(connectionFactory);
    }

    private CommonsConnectionPool createConnectionPool(DataSource unPooledDataSource, Config config) {
        CommonsConnectionPoolConfig.Builder builder = CommonsConnectionPoolConfig.Builder.newInstance();

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
