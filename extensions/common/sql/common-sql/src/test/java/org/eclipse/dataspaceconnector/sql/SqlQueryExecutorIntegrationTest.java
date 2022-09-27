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

package org.eclipse.dataspaceconnector.sql;

import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.h2.Driver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlQueryExecutorIntegrationTest {

    private Connection connection;

    static {
        Driver.load();
    }

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:test", new Properties());
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.rollback();
        connection.close();
    }

    @Test
    void testExecute() {
        SqlQueryExecutor.executeQuery(connection, "SELECT 1;");
    }

    @Test
    void testTransaction() throws SQLException {
        SqlQueryExecutor.executeQuery(connection, "SELECT COUNT(c), COUNT(*) FROM (VALUES (1), (NULL)) t(c);");

        connection.commit();
    }

    @Test
    void executeQueryStream_doesNotCloseConnection() throws SQLException {
        var connection = DriverManager.getConnection("jdbc:h2:mem:test", new Properties());

        var result = SqlQueryExecutor.executeQuery(connection, false, (rs) -> rs.getLong(1), "SELECT 1;");

        assertThat(result).isNotNull().hasSize(1).contains(1L); // assert stream closes the stream
        assertThat(connection.isClosed()).isFalse();
    }

    @Test
    void executeQueryStream_closesConnection() throws SQLException {
        var connection = DriverManager.getConnection("jdbc:h2:mem:test", new Properties());

        var result = SqlQueryExecutor.executeQuery(connection, true, (rs) -> rs.getLong(1), "SELECT 1;");

        assertThat(result).isNotNull().hasSize(1).contains(1L); // assert stream closes the stream
        assertThat(connection.isClosed()).isTrue();
    }

    @Test
    void testTransactionAndResultSetMapper() throws SQLException {
        var table = "kv_testTransactionAndResultSetMapper";
        var schema = getTableSchema(table);
        var kv = new Kv(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        SqlQueryExecutor.executeQuery(connection, schema);
        SqlQueryExecutor.executeQuery(connection, String.format("INSERT INTO %s (k, v) values (?, ?)", table), kv.key, kv.value);

        connection.commit();

        var countResult = SqlQueryExecutor.executeQuery(connection, false, (rs) -> rs.getInt(1), String.format("SELECT COUNT(*) FROM %s", table));

        assertThat(countResult).hasSize(1).first().isEqualTo(1);

        var kvs = SqlQueryExecutor.executeQuery(connection, false, (rs) -> new Kv(rs.getString(1), rs.getString(2)), String.format("SELECT * FROM %s", table));

        assertThat(kvs).hasSize(1).first().isEqualTo(kv);
    }

    @Test
    void testInvalidSql() {
        Assertions.assertThrows(EdcPersistenceException.class, () -> SqlQueryExecutor.executeQuery(connection, "Lorem ipsum dolor sit amet"));
    }

    private String getTableSchema(String tableName) {
        return String.format("" +
                "CREATE TABLE %s (\n" +
                "    k VARCHAR(80) PRIMARY KEY NOT NULL,\n" +
                "    v VARCHAR(80) NOT NULL\n" +
                ");", Objects.requireNonNull(tableName));
    }

    private static class Kv {
        final String key;
        final String value;

        Kv(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Kv kv = (Kv) o;
            return key.equals(kv.key) && Objects.equals(value, kv.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }
    }
}
