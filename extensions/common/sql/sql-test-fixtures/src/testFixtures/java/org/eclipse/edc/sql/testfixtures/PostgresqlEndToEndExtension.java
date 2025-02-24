/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.sql.testfixtures;

import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import static java.lang.String.format;
import static org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT;

/**
 * Extension to be used in end-to-end tests with Postgresql persistence
 */
public class PostgresqlEndToEndExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String DEFAULT_IMAGE = "postgres:17.3";

    private final PostgreSQLContainer<?> postgres;

    public PostgresqlEndToEndExtension() {
        this(DEFAULT_IMAGE);
    }

    public PostgresqlEndToEndExtension(String dockerImageName) {
        postgres = new PostgreSQLContainer<>(dockerImageName);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        postgres.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        postgres.stop();
        postgres.close();
    }

    /**
     * Return config suitable for EDC runtime for default database.
     *
     * @return the config.
     */
    public Config config() {
        return configFor(postgres.getDatabaseName());
    }

    /**
     * Return config suitable for EDC runtime giving the database name.
     *
     * @param databaseName the database name;
     * @return the config.
     */
    public Config configFor(String databaseName) {
        var settings = Map.of(
                "edc.datasource.default.url", "jdbc:postgresql://%s:%d/%s"
                        .formatted(postgres.getHost(), postgres.getMappedPort(POSTGRESQL_PORT), databaseName),
                "edc.datasource.default.user", postgres.getUsername(),
                "edc.datasource.default.password", postgres.getPassword(),
                "edc.sql.schema.autocreate", "true"
        );

        return ConfigFactory.fromMap(settings);
    }

    public void createDatabase(String name) {
        var jdbcUrl = postgres.getJdbcUrl() + postgres.getDatabaseName();
        try (var connection = DriverManager.getConnection(jdbcUrl, postgres.getUsername(), postgres.getPassword())) {
            connection.createStatement().execute(format("create database %s;", name));
        } catch (SQLException e) {
            // database could already exist
        }
    }

}
