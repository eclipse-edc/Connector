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

package org.eclipse.dataspaceconnector.sql.pool.commons;

import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.sql.ConnectionFactory;
import org.eclipse.dataspaceconnector.sql.datasource.ConnectionFactoryDataSource;
import org.eclipse.dataspaceconnector.sql.datasource.ConnectionPoolDataSource;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.sql.DataSource;

public class CommonsConnectionPoolServiceExtension implements ServiceExtension {
    static final String EDC_DATASOURCE_PREFIX = "edc.datasource";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    private final List<CommonsConnectionPool> commonsConnectionPools = new LinkedList<>();

    @Override
    public String name() {
        return "Commons Connection Pool";
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

    private Map<String, CommonsConnectionPool> createConnectionPools(Config parent) {
        Map<String, CommonsConnectionPool> commonsConnectionPools = new HashMap<>();
        for (Config config : parent.partition().collect(Collectors.toList())) {
            String dataSourceName = config.currentNode();

            DataSource dataSource = createDataSource(config);

            CommonsConnectionPool commonsConnectionPool = createConnectionPool(dataSource, config);
            commonsConnectionPools.put(dataSourceName, commonsConnectionPool);
        }
        return commonsConnectionPools;
    }

    private DataSource createDataSource(Config config) {
        String jdbcUrl = Objects.requireNonNull(config.getString(CommonsConnectionPoolConfigKeys.URL));

        Properties properties = new Properties();
        properties.putAll(config.getRelativeEntries());

        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(jdbcUrl, properties);

        return new ConnectionFactoryDataSource(connectionFactory);
    }

    private CommonsConnectionPool createConnectionPool(DataSource unPooledDataSource, Config config) {
        CommonsConnectionPoolConfig.Builder builder = CommonsConnectionPoolConfig.Builder.newInstance();

        setIfProvidedInt(CommonsConnectionPoolConfigKeys.POOL_MAX_IDLE_CONNECTIONS, builder::maxIdleConnections, config);
        setIfProvidedInt(CommonsConnectionPoolConfigKeys.POOL_MAX_TOTAL_CONNECTIONS, builder::maxTotalConnections, config);
        setIfProvidedInt(CommonsConnectionPoolConfigKeys.POOL_MIN_IDLE_CONNECTIONS, builder::minIdleConnections, config);
        setIfProvidedBoolean(CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_ON_BORROW, builder::testConnectionOnBorrow, config);
        setIfProvidedBoolean(CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_ON_CREATE, builder::testConnectionOnCreate, config);
        setIfProvidedBoolean(CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_ON_RETURN, builder::testConnectionOnReturn, config);
        setIfProvidedBoolean(CommonsConnectionPoolConfigKeys.POOL_TEST_CONNECTION_WHILE_IDLE, builder::testConnectionWhileIdle, config);
        setIfProvidedString(CommonsConnectionPoolConfigKeys.POOL_TEST_QUERY, builder::testQuery, config);

        return new CommonsConnectionPool(unPooledDataSource, builder.build());
    }

    private static void setIfProvidedString(String key, Consumer<String> setter, Config config) {
        var value = config.getString(key, null);
        if (value == null) {
            return;
        }
        setter.accept(value);
    }

    private static void setIfProvidedInt(String key, Consumer<Integer> setter, Config config) {
        var value = config.getInteger(key, null);
        if (value == null) {
            return;
        }

        setter.accept(value);
    }

    private static void setIfProvidedBoolean(String key, Consumer<Boolean> setter, Config config) {
        var value = config.getString(key, null);
        if (value == null) {
            return;
        }

        setter.accept(Boolean.parseBoolean(value));
    }
}
