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
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

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
    void testExecuteWithRowMapping() {
        List<Long> result = SqlQueryExecutor.executeQuery(connection, (rs) -> rs.getLong(1), "SELECT 1;");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(1, result.iterator().next());
    }

    @Test
    void testTransaction() throws SQLException {
        SqlQueryExecutor.executeQuery(connection, "SELECT COUNT(c), COUNT(*) FROM (VALUES (1), (NULL)) t(c);");

        connection.commit();
    }

    @Test
    void testTransactionAndResultSetMapper() throws SQLException {
        String table = "kv_testTransactionAndResultSetMapper";
        String schema = getTableSchema(table);
        Kv kv = new Kv(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        SqlQueryExecutor.executeQuery(connection, schema);
        SqlQueryExecutor.executeQuery(connection, String.format("INSERT INTO %s (k, v) values (?, ?)", table), kv.key, kv.value);

        connection.commit();

        List<Long> countResult = SqlQueryExecutor.executeQuery(connection, (rs) -> rs.getLong(1), String.format("SELECT COUNT(*) FROM %s", table));

        Assertions.assertNotNull(countResult);
        Assertions.assertEquals(1, countResult.size());
        Assertions.assertEquals(1, countResult.iterator().next());

        List<Kv> kvs = SqlQueryExecutor.executeQuery(connection, (rs) -> new Kv(rs.getString(1), rs.getString(2)), String.format("SELECT * FROM %s", table));

        Assertions.assertNotNull(kvs);
        Assertions.assertEquals(1, kvs.size());
        Assertions.assertEquals(kv, kvs.iterator().next());
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

        public Kv(String key, String value) {
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
