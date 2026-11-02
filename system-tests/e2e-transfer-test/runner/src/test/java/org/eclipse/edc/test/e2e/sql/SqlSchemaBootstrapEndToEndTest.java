/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.sql;

import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.Runtimes;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Verifies that a replica can start with {@code edc.sql.schema.autocreate} enabled while other replicas are using the
 * database. A schema statement that needs a lock conflicting with readers (e.g. {@code ALTER TABLE}, which takes an
 * ACCESS EXCLUSIVE lock even when there is nothing to change) would wait for every transaction using the table, while
 * all the other queries queue up behind it.
 */
@PostgresqlIntegrationTest
class SqlSchemaBootstrapEndToEndTest {

    private static final String DATABASE = Runtimes.ControlPlane.NAME.toLowerCase();

    @Order(0)
    @RegisterExtension
    static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();

    @Order(0)
    @RegisterExtension
    static final OauthServerEndToEndExtension AUTH_SERVER_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance().build();

    @Order(1)
    @RegisterExtension
    static final BeforeAllCallback SETUP = context -> POSTGRESQL_EXTENSION.createDatabase(DATABASE);

    // creates the schema
    @Order(2)
    @RegisterExtension
    static final RuntimeExtension CONTROL_PLANE = controlPlane(Runtimes.ControlPlane.NAME, () -> POSTGRESQL_EXTENSION.configFor(DATABASE));

    @Test
    void replica_shouldStart_whileTransactionReadsAllTables() throws SQLException {
        var url = POSTGRESQL_EXTENSION.getJdbcUrl(DATABASE);
        try (var reader = DriverManager.getConnection(url, POSTGRESQL_EXTENSION.getUsername(), POSTGRESQL_EXTENSION.getPassword())) {
            var statement = reader.createStatement();
            reader.setAutoCommit(false);
            var tables = new ArrayList<String>();
            try (var resultSet = statement.executeQuery("SELECT tablename FROM pg_tables WHERE schemaname = current_schema()")) {
                while (resultSet.next()) {
                    tables.add(resultSet.getString(1));
                }
            }
            assertThat(tables).isNotEmpty();
            for (var table : tables) {
                statement.execute("SELECT 1 FROM " + table + " LIMIT 1");
            }

            // the replica's schema statements give up after 1 s instead of waiting for the reader
            var replica = controlPlane(Runtimes.ControlPlane.NAME + "-replica", () -> POSTGRESQL_EXTENSION.configFor(DATABASE)
                    .merge(ConfigFactory.fromMap(Map.of("edc.datasource.default.url",
                            POSTGRESQL_EXTENSION.getJdbcUrl(DATABASE) + "?options=-c%20lock_timeout%3D1000"))));
            try {
                assertThatNoException().isThrownBy(() -> replica.beforeAll(null));
            } finally {
                replica.afterAll(null);
            }

            reader.rollback();
        }
    }

    private static ComponentRuntimeExtension controlPlane(String name, Supplier<Config> database) {
        return ComponentRuntimeExtension.Builder.newInstance()
                .name(name)
                .modules(Runtimes.ControlPlane.VIRTUAL_POLL_MODULES)
                .modules(Runtimes.ControlPlane.VIRTUAL_SQL_MODULES)
                .modules(Runtimes.ControlPlane.IAM_MOCK)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                        "edc.iam.oauth2.jwks.url", "https://example.com/jwks",
                        "edc.iam.oauth2.issuer", "test-issuer",
                        "web.http.protocol.virtual", "true",
                        "edc.dataspace.enable.profiles.all", "true")))
                .configurationProvider(database)
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .build();
    }
}
