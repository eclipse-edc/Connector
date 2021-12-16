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
 *       Daimler TSS GmbH - Initial Test
 *
 */

package org.eclipse.dataspaceconnector.sql.postgresql;

import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

@IntegrationTest
public class PostgresqlConnectionFactoryIntegrationTest {

    private static PostgresqlConnectionFactory postgresqlConnectionFactory;

    @BeforeAll
    static void setUp() {
        String host = Objects.requireNonNull(System.getenv("POSTGRES_HOST"), "POSTGRES_HOST");
        String port = Objects.requireNonNull(System.getenv("POSTGRES_PORT"), "POSTGRES_PORT");
        String databaseName = Objects.requireNonNull(System.getenv("POSTGRES_DB"), "POSTGRES_DB");
        String userName = Objects.requireNonNull(System.getenv("POSTGRES_USER"), "POSTGRES_USER");

        PostgresqlConnectionFactoryConfig postgresqlConnectionFactoryConfig = PostgresqlConnectionFactoryConfig.Builder.newInstance()
                .uri(URI.create(String.format("jdbc:postgresql://%s:%s/%s", host, port, databaseName)))
                .userName(userName)
                .password(System.getenv("POSTGRES_PASSWORD"))
                .build();

        postgresqlConnectionFactory = new PostgresqlConnectionFactory(postgresqlConnectionFactoryConfig);
    }

    @Test
    void testCreate() throws SQLException {
        try (Connection connection = postgresqlConnectionFactory.create()) {
            Assertions.assertNotNull(connection);
        }
    }

    @Test
    void testExecutePreparedStatement() throws SQLException {
        try (Connection connection = postgresqlConnectionFactory.create()) {
            Assertions.assertNotNull(connection);

            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1;")) {
                Assertions.assertTrue(preparedStatement.execute());
            }
        }
    }
}
