/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.sql.lease;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.sql.ResultSetMapper;
import org.eclipse.dataspaceconnector.transaction.local.DataSourceResource;
import org.eclipse.dataspaceconnector.transaction.local.LocalDataSourceRegistry;
import org.eclipse.dataspaceconnector.transaction.local.LocalTransactionContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

class SqlLeaseContextTest {


    private static final String DATASOURCE_NAME = "lease-test";
    private static final String LEASE_HOLDER = "test-leaser";
    private SqlLeaseContext leaseContext;
    private Connection connection;
    private TransactionContext transactionContext;
    private SqlLeaseContextBuilder builder;

    @BeforeEach
    void setUp() throws SQLException {
        var monitor = new Monitor() {
        };
        var txManager = new LocalTransactionContext(monitor);
        var dataSourceRegistry = new LocalDataSourceRegistry(txManager);
        transactionContext = txManager;
        var jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setURL("jdbc:h2:mem:");

        connection = jdbcDataSource.getConnection();
        dataSourceRegistry.register(DATASOURCE_NAME, jdbcDataSource);
        txManager.registerResource(new DataSourceResource(jdbcDataSource));

        try (var inputStream = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            var schema = new String(Objects.requireNonNull(inputStream).readAllBytes(), StandardCharsets.UTF_8);
            transactionContext.execute(() -> executeQuery(connection, schema));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var statements = new TestEntityLeaseStatements();
        builder = SqlLeaseContextBuilder.with(transactionContext, LEASE_HOLDER, statements);
        leaseContext = builder.withConnection(connection);
    }

    @Test
    void breakLease() {

        insertTestEntity("id1");
        leaseContext.acquireLease("id1");
        assertThat(isLeased("id1")).isTrue();

        leaseContext.breakLease("entityId");
        assertThat(isLeased("id1")).isTrue();
    }

    @Test
    void breakLease_whenNotExist() {
        leaseContext.breakLease("not-exist");
        //should not throw an exception
    }

    @Test
    void breakLease_whenLeaseByOther() {
        var id = "test-id";
        insertTestEntity(id);
        leaseContext.acquireLease(id);

        //break lease as someone else
        leaseContext = builder.by("someone-else").withConnection(connection);
        assertThatThrownBy(() -> leaseContext.breakLease(id)).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void acquireLease() {
        var id = "test-id";
        insertTestEntity(id);

        leaseContext.acquireLease(id);

        assertThat(isLeased(id)).isTrue();
        var leaseAssert = assertThat(leaseContext.getLease(id));
        leaseAssert.extracting(SqlLease::getLeaseId).isNotNull();
        leaseAssert.extracting(SqlLease::getLeasedBy).isEqualTo(LEASE_HOLDER);
        leaseAssert.extracting(SqlLease::getLeasedAt).matches(l -> l <= Instant.now().toEpochMilli());
        leaseAssert.extracting(SqlLease::getLeaseDuration).isEqualTo(60_000L);
    }

    @Test
    void acquireLease_leasedBySelf_throwsException() {

        var id = "test-id";
        insertTestEntity(id);

        leaseContext.acquireLease(id);
        assertThatThrownBy(() -> leaseContext.acquireLease(id)).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void acquireLease_leasedByOther_throwsException() {

        var id = "test-id";
        insertTestEntity(id);

        leaseContext.acquireLease(id);

        leaseContext = builder.by("someone-else").withConnection(connection);
        assertThatThrownBy(() -> leaseContext.acquireLease(id)).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void getLease() {
        var id = "test-id";
        insertTestEntity(id);

        leaseContext.acquireLease(id);
        assertThat(leaseContext.getLease(id)).isNotNull();
    }

    @Test
    void getLease_notExist() {
        assertThat(leaseContext.getLease("not-exist")).isNull();
    }

    private boolean isLeased(String entityId) {
        return transactionContext.execute(() -> {
            var entity = getTestEntity(entityId);
            return entity.getLeaseId() != null;
        });
    }

    private void insertTestEntity(String id) {
        transactionContext.execute(() -> {
            var stmt = "INSERT INTO edc_test_entity (id) VALUES (?);";
            executeQuery(connection, stmt, id);
        });
    }

    private TestEntity getTestEntity(String id) {
        return transactionContext.execute(() -> {
            var stmt = "SELECT * FROM edc_test_entity WHERE id=?";

            var res = executeQuery(connection, map(), stmt, id);

            return res.stream().findFirst().orElse(null);
        });
    }


    private ResultSetMapper<TestEntity> map() {
        return (rs) -> new TestEntity(rs.getString("id"), rs.getString("lease_id"));
    }

    private static class TestEntity {
        private final String id;
        private final String leaseId;

        public TestEntity(String id, String leaseId) {
            this.id = id;
            this.leaseId = leaseId;
        }

        public String getId() {
            return id;
        }

        public String getLeaseId() {
            return leaseId;
        }
    }

    private static class TestEntityLeaseStatements implements LeaseStatements {

        @Override
        public String getDeleteLeaseTemplate() {
            return "DELETE FROM edc_lease WHERE lease_id=?;";
        }

        @Override
        public String getInsertLeaseTemplate() {
            return "INSERT INTO edc_lease (lease_id, leased_by, leased_at, lease_duration) VALUES (?, ?, ?, ?);";
        }

        @Override
        public String getUpdateLeaseTemplate() {
            return "UPDATE edc_test_entity SET lease_id=? WHERE id = ?;";
        }

        @Override
        public String getFindLeaseByEntityTemplate() {
            return "SELECT * FROM edc_lease WHERE lease_id = (SELECT lease_id FROM edc_test_entity WHERE id=?)";
        }
    }
}