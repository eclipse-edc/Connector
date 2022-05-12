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

package org.eclipse.dataspaceconnector.test.e2e.postgresql;

import java.sql.DriverManager;
import java.sql.SQLException;

import static java.lang.String.format;

public interface PostgresqlLocalInstance {
    String USER = "postgres";
    String PASSWORD = "password";
    String JDBC_URL_PREFIX = "jdbc:postgresql://localhost:5432/";

    static void createDatabase(String name) {
        try (var connection = DriverManager.getConnection(JDBC_URL_PREFIX + USER, USER, PASSWORD)) {
            connection.createStatement().execute(format("create database %s;", name));
        } catch (SQLException e) {
            // database could already exist
        }
    }
}
