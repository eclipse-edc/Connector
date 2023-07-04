/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Test
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.sql;

import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class SqlQueryExecutorIntegrationTest {

    private final SqlQueryExecutor executor = new SqlQueryExecutor();
    private final String table = "key_value";

    @BeforeEach
    void setUp(Connection connection) {
        executor.execute(connection, format("""
                CREATE TABLE %s (
                    k VARCHAR(80) PRIMARY KEY NOT NULL,
                    v VARCHAR(80) NOT NULL
                );""", Objects.requireNonNull(table)));
    }

    @AfterEach
    void tearDown(Connection connection) {
        executor.execute(connection, format("DROP TABLE %s", table));
    }

    @Test
    void executeQuery_doesNotCloseConnection(Connection connection) throws SQLException {
        ResultSetMapper<Long> mapper = (rs) -> rs.getLong(1);
        var sql = "SELECT 1;";

        var result = executor.query(connection, false, mapper, sql);

        assertThat(result).isNotNull().hasSize(1).contains(1L);
        verify(connection, never()).close();
    }

    @Test
    void executeQuery_closesConnection(Connection connection) throws SQLException {
        ResultSetMapper<Long> mapper = (rs) -> rs.getLong(1);
        var sql = "SELECT 1;";

        var result = executor.query(connection, true, mapper, sql);

        assertThat(result).isNotNull().hasSize(1).contains(1L);
        verify(connection).close();
    }

    @Test
    void executeQuerySingle(Connection connection) {
        var sql = "SELECT v FROM key_value WHERE k = ?";
        var keyValue = insertRow(connection);
        ResultSetMapper<String> mapper = rs -> rs.getString(1);

        var found = executor.single(connection, false, mapper, sql, keyValue.key);
        assertThat(found).isEqualTo(keyValue.value);

        var notFound = executor.single(connection, false, mapper, sql, "any other");
        assertThat(notFound).isEqualTo(null);
    }

    @Test
    void testTransactionAndResultSetMapper(Connection connection) {
        var keyValue = insertRow(connection);

        var countResult = executor.query(connection, false, (rs) -> rs.getInt(1), format("SELECT COUNT(*) FROM %s", table));
        assertThat(countResult).hasSize(1).first().isEqualTo(1);

        var kvs = executor.query(connection, false, (rs) -> new KeyValue(rs.getString(1), rs.getString(2)), format("SELECT * FROM %s", table));
        assertThat(kvs).hasSize(1).first().isEqualTo(keyValue);
    }

    @Test
    void testInvalidSql(Connection connection) {
        assertThatThrownBy(() -> executor.execute(connection, "Lorem ipsum dolor sit amet")).isInstanceOf(EdcPersistenceException.class);
    }

    @NotNull
    private KeyValue insertRow(Connection connection) {
        var keyValue = new KeyValue(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        executor.execute(connection, format("INSERT INTO %s (k, v) values (?, ?)", table), keyValue.key, keyValue.value);
        return keyValue;
    }

    private record KeyValue(String key, String value) {
    }
}
