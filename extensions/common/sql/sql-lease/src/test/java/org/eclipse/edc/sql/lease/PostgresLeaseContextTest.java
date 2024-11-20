/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       SAP SE - bugfix (pass correct lease id for deletion)
 *
 */

package org.eclipse.edc.sql.lease;

import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.sql.ResultSetMapper;
import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class PostgresLeaseContextTest {

    protected static final String LEASE_HOLDER = "test-leaser";
    protected final Instant now = Clock.systemUTC().instant();

    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final TestEntityLeaseStatements dialect = new TestEntityLeaseStatements();
    private final SqlQueryExecutor queryExecutor = new SqlQueryExecutor();
    private SqlLeaseContextBuilder builder;
    private SqlLeaseContext leaseContext;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension setupExtension, Connection connection) throws IOException {
        var schema = Files.readString(Paths.get("./src/test/resources/schema.sql"));
        setupExtension.runQuery(schema);

        builder = SqlLeaseContextBuilder.with(transactionContext, LEASE_HOLDER, dialect, Clock.fixed(now, UTC), queryExecutor);
        leaseContext = builder.by(LEASE_HOLDER).withConnection(connection);
    }

    @AfterEach
    void teardown(PostgresqlStoreSetupExtension setupExtension) {
        setupExtension.runQuery("DROP TABLE " + dialect.getLeaseTableName() + " CASCADE");
        setupExtension.runQuery("DROP TABLE " + dialect.getEntityTableName() + " CASCADE");
    }

    @Test
    void breakLease(Connection connection) {
        insertTestEntity("id1", connection);
        leaseContext.acquireLease("id1");
        assertThat(isLeased("id1", connection)).isTrue();

        leaseContext.breakLease("entityId");
        assertThat(isLeased("id1", connection)).isTrue();
    }

    @Test
    void breakLease_whenNotExist() {
        leaseContext.breakLease("not-exist");
        //should not throw an exception
    }

    @Test
    void breakLease_whenLeaseByOther(Connection connection) {
        var id = "test-id";
        insertTestEntity(id, connection);
        leaseContext.acquireLease(id);

        //break lease as someone else
        var leaseContext = builder.by("someone-else").withConnection(connection);
        assertThatThrownBy(() -> leaseContext.breakLease(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acquireLease(Connection connection) {
        var id = "test-id";
        insertTestEntity(id, connection);

        leaseContext.acquireLease(id);

        assertThat(isLeased(id, connection)).isTrue();
        var leaseAssert = assertThat(leaseContext.getLease(id));
        leaseAssert.extracting(SqlLease::getLeaseId).isNotNull();
        leaseAssert.extracting(SqlLease::getLeasedBy).isEqualTo(LEASE_HOLDER);
        leaseAssert.extracting(SqlLease::getLeasedAt).matches(l -> l <= now.toEpochMilli());
        leaseAssert.extracting(SqlLease::getLeaseDuration).isEqualTo(60_000L);
    }

    @Test
    void acquireLease_leasedBySelf_throwsException(Connection connection) {

        var id = "test-id";
        insertTestEntity(id, connection);

        leaseContext.acquireLease(id);
        assertThatThrownBy(() -> leaseContext.acquireLease(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acquireLease_leasedByOther_throwsException(Connection connection) {

        var id = "test-id";
        insertTestEntity(id, connection);

        leaseContext.acquireLease(id);

        var leaseContext = builder.by("someone-else").withConnection(connection);
        assertThatThrownBy(() -> leaseContext.acquireLease(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getLease(Connection connection) {
        var id = "test-id";
        insertTestEntity(id, connection);

        leaseContext.acquireLease(id);
        assertThat(leaseContext.getLease(id)).isNotNull();
    }

    @Test
    void getLease_notExist() {
        assertThat(leaseContext.getLease("not-exist")).isNull();
    }

    @Test
    void acquireLease_whenExpiredLeasePresent_shouldDeleteOldLeaseAndAcquireNewLease(Connection connection) {
        var entityId = "test-entity";
        insertTestEntity(entityId, connection);
        var leaseContext = builder.by("someone-else").withConnection(connection);

        // no lease present, acquire one
        leaseContext.acquireLease(entityId);
        var lease = leaseContext.getLease(entityId);
        assertThat(lease).isNotNull();
        var leaseId = lease.getLeaseId();

        // should acquire lease by deleting old one after lease expiry
        var twoMinutesAheadClock = Clock.offset(Clock.fixed(now, UTC), Duration.of(2, ChronoUnit.MINUTES));
        var twoMinutesAheadBuilder = SqlLeaseContextBuilder.with(transactionContext, LEASE_HOLDER, dialect, twoMinutesAheadClock, queryExecutor);
        var twoMinutesAheadContext = twoMinutesAheadBuilder.by("someone-else").withConnection(connection);
        twoMinutesAheadContext.acquireLease(entityId);

        var newLease = twoMinutesAheadContext.getLease(entityId);
        assertThat(newLease).isNotNull();
        assertThat(newLease.getLeaseId()).isNotEqualTo(leaseId);
    }

    protected boolean isLeased(String entityId, Connection connection) {
        return transactionContext.execute(() -> {
            var entity = getTestEntity(entityId, connection);
            return entity.getLeaseId() != null;
        });
    }

    protected void insertTestEntity(String id, Connection connection) {
        transactionContext.execute(() -> {
            var stmt = "INSERT INTO " + dialect.getEntityTableName() + " (id) VALUES (?);";
            queryExecutor.execute(connection, stmt, id);
        });
    }

    protected TestEntity getTestEntity(String id, Connection connection) {
        return transactionContext.execute(() -> {
            var stmt = "SELECT * FROM " + dialect.getEntityTableName() + " WHERE id=?";

            try (var stream = queryExecutor.query(connection, false, map(), stmt, id)) {
                return stream.findFirst().orElse(null);
            }
        });
    }

    private ResultSetMapper<TestEntity> map() {
        return (rs) -> new TestEntity(rs.getString("id"), rs.getString("lease_id"));
    }

    private static class TestEntityLeaseStatements implements LeaseStatements {

        @Override
        public String getDeleteLeaseTemplate() {
            return executeStatement().delete(getLeaseTableName(), getLeaseIdColumn());
        }

        @Override
        public String getInsertLeaseTemplate() {
            return executeStatement()
                    .column(getLeaseIdColumn())
                    .column(getLeasedByColumn())
                    .column(getLeasedAtColumn())
                    .column(getLeaseDurationColumn())
                    .insertInto(getLeaseTableName());
        }

        @Override
        public String getUpdateLeaseTemplate() {
            return executeStatement()
                    .column(getLeaseIdColumn())
                    .update(getEntityTableName(), "id");
        }

        @Override
        public String getFindLeaseByEntityTemplate() {
            return "SELECT * FROM edc_lease WHERE lease_id = (SELECT lease_id FROM " + getEntityTableName() + " WHERE id=?)";
        }

        public String getEntityTableName() {
            return "edc_test_entity";
        }
    }

    protected static class TestEntity {
        private final String id;
        private final String leaseId;

        TestEntity(String id, String leaseId) {
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

}
