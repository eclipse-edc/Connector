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

package org.eclipse.dataspaceconnector.clients.postgresql;

import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactory;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactoryConfig;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactoryImpl;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.ConnectionPool;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.commons.CommonsConnectionPool;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.commons.CommonsConnectionPoolConfig;
import org.eclipse.dataspaceconnector.clients.postgresql.row.RowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Testcontainers
class PostgresqlClientImplTest {
    private PostgresqlClient postgresqlClient;

    @Container
    @SuppressWarnings("rawtypes")
    private static final PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer("postgres:9.6.12");

    @BeforeEach
    public void setUp() {
        ConnectionFactoryConfig connectionFactoryConfig = ConnectionFactoryConfig.Builder.newInstance()
                .uri(URI.create(POSTGRES_CONTAINER.getJdbcUrl()))
                .userName(POSTGRES_CONTAINER.getUsername())
                .password(POSTGRES_CONTAINER.getPassword())
                .build();

        ConnectionFactory connectionFactory = new ConnectionFactoryImpl(connectionFactoryConfig);

        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance()
                .build();

        ConnectionPool connectionPool = new CommonsConnectionPool(connectionFactory, commonsConnectionPoolConfig);

        postgresqlClient = new PostgresqlClientImpl(connectionPool);
    }

    @AfterEach
    public void tearDown() throws Exception {
        postgresqlClient.close();
    }

    private String getTableSchema(String tableName) {
        return String.format("" +
                "CREATE TABLE %s (\n" +
                "    k VARCHAR(80) PRIMARY KEY NOT NULL,\n" +
                "    v VARCHAR(80) NOT NULL\n" +
                ");", Objects.requireNonNull(tableName));
    }

    @Test
    void testExecute() throws SQLException {
        postgresqlClient.execute("SELECT 1;");
    }

    @Test
    void testExecuteWithRowMapping() throws SQLException {
        List<Long> result = postgresqlClient.execute(CountRowMapper.INSTANCE, "SELECT 1;");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(1, result.iterator().next());
    }

    @Test
    void testTransaction() throws SQLException {
        postgresqlClient.doInTransaction(client -> client.execute("SELECT COUNT(c), COUNT(*) FROM (VALUES (1), (NULL)) t(c);"));
    }

    @Test
    void testNestedTransactionUnsupported() throws SQLException {
        String table = "kv_testNestedTransactionUnsupported";
        String schema = getTableSchema(table);

        postgresqlClient.doInTransaction(client -> client.execute(schema));

        Assertions.assertThrows(SQLException.class, () -> postgresqlClient.doInTransaction(client -> {
            client.doInTransaction(c -> c.execute(String.format("INSERT INTO %s (k, v) values (?, ?)", table), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        }));

        List<Long> countResult = postgresqlClient.execute(CountRowMapper.INSTANCE, String.format("SELECT COUNT(*) FROM %s", table));
        Assertions.assertNotNull(countResult);
        Assertions.assertEquals(1, countResult.size());
        Assertions.assertEquals(0, countResult.iterator().next());
    }

    @Test
    void testTransactionRollbackOnRuntimeException() throws SQLException {
        String table = "kv_testTransactionRollbackOnRuntimeException";
        String schema = getTableSchema(table);

        postgresqlClient.doInTransaction(client -> client.execute(schema));

        Assertions.assertThrows(SQLException.class, () -> postgresqlClient.doInTransaction(
                client -> {
                    client.execute(String.format("INSERT INTO %s (k, v) values (?, ?)", table), UUID.randomUUID().toString(), UUID.randomUUID().toString());

                    throw new RuntimeException("Intended");
                }));

        List<Long> countResult = postgresqlClient.execute(CountRowMapper.INSTANCE, String.format("SELECT COUNT(*) FROM %s", table));
        Assertions.assertNotNull(countResult);
        Assertions.assertEquals(1, countResult.size());
        Assertions.assertEquals(0, countResult.iterator().next());
    }

    @Test
    void testTransactionRollbackOnSqlException() throws SQLException {
        String table = "kv_testTransactionRollbackOnSqlException";
        String schema = getTableSchema(table);

        postgresqlClient.doInTransaction(client -> client.execute(schema));

        Assertions.assertThrows(SQLException.class, () -> postgresqlClient.doInTransaction(
                client -> {
                    client.execute(String.format("INSERT INTO %s (k, v) values (?, ?)", table), UUID.randomUUID().toString(), UUID.randomUUID().toString());

                    throw new SQLException("Intended");
                }));

        List<Long> countResult = postgresqlClient.execute(CountRowMapper.INSTANCE, String.format("SELECT COUNT(*) FROM %s", table));
        Assertions.assertNotNull(countResult);
        Assertions.assertEquals(1, countResult.size());
        Assertions.assertEquals(0, countResult.iterator().next());
    }

    @Test
    void testTransactionRollbackOnFailingQuery() throws SQLException {
        String table = "kv_testTransactionRollbackOnFailingQuery";
        String schema = getTableSchema(table);

        postgresqlClient.doInTransaction(client -> client.execute(schema));

        Assertions.assertThrows(SQLException.class, () -> postgresqlClient.doInTransaction(
                client -> {
                    client.execute(String.format("INSERT INTO %s (k, v) values (?, ?)", table), UUID.randomUUID().toString(), UUID.randomUUID().toString());

                    client.doInTransaction(c2 -> c2.execute("BREAK TX"));
                }));

        List<Long> countResult = postgresqlClient.execute(CountRowMapper.INSTANCE, String.format("SELECT COUNT(*) FROM %s", table));
        Assertions.assertNotNull(countResult);
        Assertions.assertEquals(1, countResult.size());
        Assertions.assertEquals(0, countResult.iterator().next());
    }

    @Test
    void testRowMapper() throws SQLException {
        String table = "kv_testRowMapper";
        String schema = getTableSchema(table);

        postgresqlClient.doInTransaction(client -> client.execute(schema));

        Tuple tuple = new Tuple(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        postgresqlClient.doInTransaction(client -> client.execute(
                String.format("INSERT INTO %s (k, v) values (?, ?)", table), tuple.key, tuple.value));

        RowMapper<Tuple> rowMapper = (resultSet) -> new Tuple(resultSet.getString("k"), resultSet.getString("v"));
        List<Tuple> tuples = postgresqlClient.execute(rowMapper, String.format("SELECT * FROM %s", table));

        Assertions.assertNotNull(tuples);
        Assertions.assertEquals(1, tuples.size());
        Assertions.assertEquals(tuple, tuples.iterator().next());
    }

    @Test
    void testInvalidSql() {
        Assertions.assertThrows(SQLException.class, () -> postgresqlClient.execute("Lorem ipsum dolor sit amet"));
    }

    @Test
    void testIn() throws SQLException {
        String table = "kv_testIn";
        String schema = getTableSchema(table);

        List<String> ks = new LinkedList<>();
        postgresqlClient.doInTransaction(client -> {
            client.execute(schema);
            String k1 = UUID.randomUUID().toString();
            ks.add(k1);
            String k2 = UUID.randomUUID().toString();
            ks.add(k2);
            String k3 = UUID.randomUUID().toString();
            ks.add(k3);
            client.execute(String.format("INSERT INTO %s (k, v) values (?, ?)", table), k1, UUID.randomUUID().toString());
            client.execute(String.format("INSERT INTO %s (k, v) values (?, ?)", table), k2, UUID.randomUUID().toString());
            client.execute(String.format("INSERT INTO %s (k, v) values (?, ?)", table), k3, UUID.randomUUID().toString());
        });

        List<Long> countResult = postgresqlClient.execute(
                CountRowMapper.INSTANCE,
                String.format("SELECT COUNT(*) FROM %s WHERE k in (%s)", table, String.join(",", Collections.nCopies(ks.size(), "?"))),
                ks.toArray());
        Assertions.assertNotNull(countResult);
        Assertions.assertEquals(1, countResult.size());
        Assertions.assertEquals(3, countResult.iterator().next());
    }

    private enum CountRowMapper implements RowMapper<Long> {
        INSTANCE;

        @Override
        public Long mapRow(ResultSet resultSet) throws SQLException {
            return resultSet.getLong(1);
        }
    }

    private static class Tuple {
        final String key;
        final String value;

        public Tuple(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple tuple = (Tuple) o;
            return key.equals(tuple.key) && Objects.equals(value, tuple.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }
    }
}
