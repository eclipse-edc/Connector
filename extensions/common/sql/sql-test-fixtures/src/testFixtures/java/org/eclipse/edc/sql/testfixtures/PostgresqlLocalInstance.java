/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.sql.testfixtures;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public final class PostgresqlLocalInstance {
    private final String jdbcUrlPrefix;
    private final String username;
    private final String password;

    public PostgresqlLocalInstance(String username, String password, String jdbcUrlPrefix) {
        this.username = username;
        this.password = password;
        this.jdbcUrlPrefix = jdbcUrlPrefix;
    }

    public void createDatabase(String name) {
        try (var connection = getConnection("postgres")) {
            connection.createStatement().execute(format("create database %s;", name));
        } catch (SQLException e) {
            // database could already exist
        }
    }

    public Connection getConnection(String databaseName) {
        try {
            return DriverManager.getConnection(jdbcUrlPrefix + databaseName, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> createDefaultDatasourceConfiguration(String databaseName) {
        return new HashMap<>() {
            {
                put("edc.datasource.default.url", jdbcUrlPrefix + databaseName);
                put("edc.datasource.default.user", username);
                put("edc.datasource.default.password", password);
            }
        };
    }

}
