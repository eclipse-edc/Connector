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

package org.eclipse.dataspaceconnector.postgresql.contractdefinitionloader;

import org.eclipse.dataspaceconnector.clients.postgresql.PostgresqlClient;
import org.eclipse.dataspaceconnector.clients.postgresql.PostgresqlClientImpl;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.Repository;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.RepositoryImpl;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactory;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactoryConfig;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactoryImpl;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.ConnectionPool;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.commons.CommonsConnectionPool;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.commons.CommonsConnectionPoolConfig;
import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.postgresql.contractdefinitionloader.settings.CommonsConnectionPoolConfigFactory;
import org.eclipse.dataspaceconnector.postgresql.contractdefinitionloader.settings.ConnectionFactoryConfigFactory;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PostgresqlServiceExtensionImpl implements ServiceExtension {

    private static final String NAME = "PostgreSql Contract Definition Loader Service Extension";

    @Override
    public Set<String> provides() {
        return Set.of(ContractDefinitionLoader.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        ContractDefinitionLoader assetIndex = createContractDefinitionLoader(context);
        context.registerService(ContractDefinitionLoader.class, assetIndex);

        context.getMonitor().info(String.format("Initialized %s", NAME));
    }

    @NotNull
    private ContractDefinitionLoader createContractDefinitionLoader(ServiceExtensionContext context) {
        ConnectionFactoryConfigFactory connectionFactoryConfigFactory = new ConnectionFactoryConfigFactory(context);
        ConnectionFactoryConfig connectionFactoryConfig = connectionFactoryConfigFactory.create();
        ConnectionFactory connectionFactory = new ConnectionFactoryImpl(connectionFactoryConfig);
        CommonsConnectionPoolConfigFactory commonsConnectionPoolConfigFactory = new CommonsConnectionPoolConfigFactory(context);
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = commonsConnectionPoolConfigFactory.create();
        ConnectionPool connectionPool = new CommonsConnectionPool(connectionFactory, commonsConnectionPoolConfig);
        PostgresqlClient postgresqlClient = new PostgresqlClientImpl(connectionPool);
        Repository assetRepository = new RepositoryImpl(postgresqlClient);
        return new PostgresContractDefinitionLoader(assetRepository);
    }
}
