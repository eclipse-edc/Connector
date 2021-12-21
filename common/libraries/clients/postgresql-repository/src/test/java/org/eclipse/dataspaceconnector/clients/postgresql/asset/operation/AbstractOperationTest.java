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

package org.eclipse.dataspaceconnector.clients.postgresql.asset.operation;

import org.eclipse.dataspaceconnector.clients.postgresql.PostgresqlClient;
import org.eclipse.dataspaceconnector.clients.postgresql.PostgresqlClientImpl;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.Repository;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.RepositoryImpl;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.util.TestPreparedStatementResourceReader;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactory;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactoryConfig;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactoryImpl;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.ConnectionPool;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.commons.CommonsConnectionPool;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.commons.CommonsConnectionPoolConfig;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.sql.SQLException;
import java.util.UUID;

@Testcontainers
public class AbstractOperationTest {

    @Container
    @SuppressWarnings("rawtypes")
    private static final PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer("postgres:9.6.12");

    private PostgresqlClient postgresqlClient;
    private Repository repository;

    protected Repository getRepository() {
        return repository;
    }

    protected PostgresqlClient getClient() {
        return postgresqlClient;
    }

    protected Asset createUniqueAsset() {
        return Asset.Builder.newInstance().id(UUID.randomUUID().toString()).build();
    }

    @BeforeEach
    final void setupPostgresAndClient() throws SQLException {

        ConnectionFactoryConfig connectionFactoryConfig = ConnectionFactoryConfig.Builder.newInstance()
                .uri(URI.create(POSTGRES_CONTAINER.getJdbcUrl()))
                .userName(POSTGRES_CONTAINER.getUsername())
                .password(POSTGRES_CONTAINER.getPassword())
                .build();

        ConnectionFactory connectionFactory = new ConnectionFactoryImpl(connectionFactoryConfig);

        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance()
                .build();

        ConnectionPool connectionPool = new CommonsConnectionPool(connectionFactory, commonsConnectionPoolConfig);

        PostgresqlClient client = new PostgresqlClientImpl(connectionPool);

        client.execute(TestPreparedStatementResourceReader.getTablesCreate());

        this.postgresqlClient = client;
        this.repository = new RepositoryImpl(client);
    }

    @AfterEach
    final void teardownPostgresAndClient() throws Exception {
        postgresqlClient.execute(TestPreparedStatementResourceReader.getTablesDelete());
        postgresqlClient.close();
    }

}
