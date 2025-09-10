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
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.sql.ResultSetMapper;
import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class PostgresLeaseContextTest {

    protected static final String LEASE_HOLDER = "test-leaser";
    protected final Instant now = Clock.systemUTC().instant();

    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final TestEntityLeaseStatements dialect = new TestEntityLeaseStatements();
    private final SqlQueryExecutor queryExecutor = new SqlQueryExecutor();
    private SqlLeaseContextBuilderImpl builder;
    private SqlLeaseContext leaseContext;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension setupExtension, Connection connection) throws IOException {
        var schema = Files.readString(Paths.get("./src/test/resources/schema.sql"));
        setupExtension.runQuery(schema);

        builder = SqlLeaseContextBuilderImpl.with(transactionContext, LEASE_HOLDER, "TestTarget", dialect, Clock.fixed(now, UTC), queryExecutor);
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
        leaseContext.acquireLease("id1").orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        assertThat(isLeased("id1")).isTrue();

        leaseContext.breakLease("entityId").orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        assertThat(isLeased("id1")).isTrue();
    }

    @Test
    void breakLease_whenNotExist() {
        leaseContext.breakLease("not-exist").orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        //should not throw an exception
    }

    @Test
    void breakLease_whenLeaseByOther(Connection connection) {
        var id = "test-id";
        insertTestEntity(id, connection);
        leaseContext.acquireLease(id).orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        //break lease as someone else
        var leaseContext = builder.by("someone-else").withConnection(connection);
        assertThat(leaseContext.acquireLease(id)).isFailed()
                .extracting(StoreFailure::getReason)
                .isEqualTo(StoreFailure.Reason.ALREADY_LEASED);
    }

    @Test
    void acquireLease(Connection connection) {
        var id = "test-id";
        insertTestEntity(id, connection);

        leaseContext.acquireLease(id).orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        assertThat(isLeased(id)).isTrue();

        var lease = leaseContext.getLease(id);
        assertThat(lease).isNotNull();
        assertThat(lease.getResourceId()).isEqualTo(id);
        assertThat(lease.getLeasedBy()).isEqualTo(LEASE_HOLDER);
        assertThat(lease.getResourceKind()).isEqualTo("TestTarget");
        assertThat(lease.getLeasedAt()).matches(l -> l <= now.toEpochMilli());
        assertThat(lease.getLeaseDuration()).isEqualTo(60_000L);
    }

    @Test
    void acquireLease_leasedBySelf_throwsException(Connection connection) {

        var id = "test-id";
        insertTestEntity(id, connection);

        leaseContext.acquireLease(id).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        assertThat(leaseContext.acquireLease(id)).isFailed()
                .extracting(StoreFailure::getReason)
                .isEqualTo(StoreFailure.Reason.ALREADY_LEASED);
    }

    @Test
    void acquireLease_leasedByOther_throwsException(Connection connection) {

        var id = "test-id";
        insertTestEntity(id, connection);

        leaseContext.acquireLease(id).orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var leaseContext = builder.by("someone-else").withConnection(connection);

        assertThat(leaseContext.acquireLease(id)).isFailed()
                .extracting(StoreFailure::getReason)
                .isEqualTo(StoreFailure.Reason.ALREADY_LEASED);
    }

    @Test
    void getLease(Connection connection) {
        var id = "test-id";
        insertTestEntity(id, connection);

        leaseContext.acquireLease(id).orElseThrow(f -> new AssertionError(f.getFailureDetail()));

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
        leaseContext.acquireLease(entityId).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        var lease = leaseContext.getLease(entityId);
        assertThat(lease).isNotNull();
        var leaseBy = lease.getLeasedBy();

        // should acquire lease by deleting old one after lease expiry
        var twoMinutesAheadClock = Clock.offset(Clock.fixed(now, UTC), Duration.of(2, ChronoUnit.MINUTES));
        var twoMinutesAheadBuilder = SqlLeaseContextBuilderImpl.with(transactionContext, LEASE_HOLDER, "TestTarget", dialect, twoMinutesAheadClock, queryExecutor);
        var twoMinutesAheadContext = twoMinutesAheadBuilder.withConnection(connection);

        twoMinutesAheadContext.acquireLease(entityId).orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var newLease = twoMinutesAheadContext.getLease(entityId);
        assertThat(newLease).isNotNull();
        assertThat(newLease.getLeasedBy()).isNotEqualTo(leaseBy);
    }


    @Test
    void acquireAndReleaseLease_WithMultipleLeaseHolders(PostgresqlStoreSetupExtension extension) throws InterruptedException {
        var concurrency = 20;
        var executor = Executors.newFixedThreadPool(concurrency);
        var entityId = "test-entity";

        var completionLatch = new CountDownLatch(concurrency);
        var releaseLatch = new CountDownLatch(concurrency - 1);

        var acquireBarrier = new CyclicBarrier(concurrency);
        var breakBarrier = new CyclicBarrier(concurrency);

        var notAcquired = new AtomicInteger(0);
        var notReleased = new AtomicInteger(0);
        var acquiredHolder = new AtomicReference<String>();
        var releaseHolder = new AtomicReference<String>();

        for (var i = 0; i < concurrency; i++) {
            int leaseNumber = i;
            executor.execute(() -> {
                var leaseHolder = LEASE_HOLDER + leaseNumber;
                var builder = SqlLeaseContextBuilderImpl.with(transactionContext, leaseHolder, "TestTarget", dialect, Clock.fixed(now, UTC), queryExecutor);
                var leaseContext = builder.withConnection(extension.getConnection());
                try {
                    acquireBarrier.await();
                    // try to acquire the lease concurrently
                    if (acquireLease(leaseContext, entityId)) {
                        acquiredHolder.set(leaseHolder);
                    } else {
                        notAcquired.incrementAndGet();
                    }
                    breakBarrier.await();
                    // try to release the lease concurrently. Only the holder should be able to do it.
                    if (!leaseHolder.equals(acquiredHolder.get())) {
                        if (!breakLease(leaseContext, entityId)) {
                            notReleased.incrementAndGet();
                        }
                        releaseLatch.countDown();
                    } else {
                        releaseLatch.await();
                        if (breakLease(leaseContext, entityId)) {
                            releaseHolder.set(leaseHolder);
                        } else {
                            notReleased.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    completionLatch.countDown();
                }
            });

        }

        completionLatch.await();

        assertThat(notAcquired.get()).isEqualTo(concurrency - 1);
        assertThat(notReleased.get()).isEqualTo(concurrency - 1);
        assertThat(acquiredHolder.get()).isEqualTo(releaseHolder.get());


        var query = "SELECT count(*) FROM " + dialect.getLeaseTableName();
        var count = queryExecutor.single(extension.getConnection(), true, r -> r.getLong(1), query);

        assertThat(count).isEqualTo(0);
    }

    protected boolean acquireLease(SqlLeaseContext leaseContext, String entityId) {
        return leaseContext.acquireLease(entityId).succeeded();
    }

    protected boolean breakLease(SqlLeaseContext leaseContext, String entityId) {
        return leaseContext.breakLease(entityId).succeeded();
    }

    protected boolean isLeased(String entityId) {
        return transactionContext.execute(() -> {
            var entity = leaseContext.getLease(entityId);
            return entity != null;
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
