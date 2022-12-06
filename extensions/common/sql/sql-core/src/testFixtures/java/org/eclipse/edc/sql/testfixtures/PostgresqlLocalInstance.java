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

import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;

import static java.lang.String.format;

public final class PostgresqlLocalInstance {
    public static final String USER = "postgres";
    public static final String PASSWORD = "password";
    public static final String JDBC_URL_PREFIX = "jdbc:postgresql://localhost:5432/";
    private static final String TEST_DATABASE = "itest";

    private PostgresqlLocalInstance() { }

    public static void createTestDatabase() {
        createDatabase(TEST_DATABASE);
    }

    public static void createDatabase(String name) {
        try (var connection = DriverManager.getConnection(JDBC_URL_PREFIX + USER, USER, PASSWORD)) {
            connection.createStatement().execute(format("create database %s;", name));
        } catch (SQLException e) {
            // database could already exist
        }
    }

    public static Connection getTestConnection() {
        try {
            return createTestDataSource().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static DataSource createTestDataSource() {
        var dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[]{ "localhost" });
        dataSource.setPortNumbers(new int[]{ 5432 });
        dataSource.setUser(USER);
        dataSource.setPassword(PASSWORD);
        dataSource.setDatabaseName(TEST_DATABASE);
        return dataSource;
    }
}
