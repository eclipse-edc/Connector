/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.transferprocess;

import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.SqlTransferProcessStore;
import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.sql.testfixtures.RowLeaseUtil;
import org.eclipse.edc.transaction.local.LocalDataSourceRegistry;
import org.eclipse.edc.transaction.local.LocalTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.testfixtures.store.TestFunctions.createTransferProcess;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_LEASED;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.CONFLICT;

/**
 * Simulates multiple runtime replicas running the transfer process state machine against the same database.
 * <p>
 * It uses the production {@link LocalTransactionContext} instead of the {@code NoopTransactionContext} of
 * {@link PostgresqlStoreSetupExtension}, so that the {@code FOR UPDATE SKIP LOCKED} row locks taken by
 * {@link SqlTransferProcessStore#nextNotLeased} are held until the enclosing transaction completes.
 */
@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlTransferProcessStoreConcurrencyTest {

    private static final int REPLICAS = 5;
    private static final int ENTITIES = 300;
    private static final int BATCH_SIZE = 20;

    private final PostgresDialectStatements statements = new PostgresDialectStatements(Clock.systemUTC());
    private final Clock clock = Clock.systemUTC();
    private final JacksonTypeManager typeManager = new JacksonTypeManager();
    private LocalTransactionContext transactionContext;
    private LocalDataSourceRegistry dataSourceRegistry;
    private PostgresqlStoreSetupExtension extension;
    private QueryExecutor queryExecutor;
    private RowLeaseUtil leaseUtil;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        this.extension = extension;
        this.queryExecutor = queryExecutor;
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));
        transactionContext = new LocalTransactionContext(new ConsoleMonitor());
        dataSourceRegistry = new LocalDataSourceRegistry(transactionContext);
        // DefaultDataSourceRegistry.resolve() hands back the raw DataSource, which the local registry then enlists
        dataSourceRegistry.register(extension.getDatasourceName(), extension.getDataSourceRegistry().resolve(extension.getDatasourceName()));
        leaseUtil = new RowLeaseUtil(transactionContext, this::getConnection, statements, statements.getTransferProcessTableName(), clock);

        extension.runQuery(TestUtils.getResourceFileContentAsString("transfer-process-schema.sql"));
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getTransferProcessTableName() + " CASCADE");
    }

    @Test
    void multipleReplicas_shouldProcessEachEntityExactlyOnce() throws Exception {
        var seedStore = createStore("seed");
        var ids = IntStream.range(0, ENTITIES).mapToObj(i -> createTransferProcess(UUID.randomUUID().toString(), INITIAL)).peek(seedStore::save).map(TransferProcess::getId).toList();

        var processed = new ConcurrentHashMap<String, AtomicInteger>();
        var barrier = new CyclicBarrier(REPLICAS);
        var executor = Executors.newFixedThreadPool(REPLICAS);

        try {
            var replicas = IntStream.range(0, REPLICAS).mapToObj(replica -> (Callable<Void>) () -> {
                var store = createStore("replica-" + replica);
                barrier.await(30, TimeUnit.SECONDS);
                // mirrors the state machine processor: lease a batch, then process and save every entity in its own transaction
                List<TransferProcess> entities;
                do {
                    entities = store.nextNotLeased(BATCH_SIZE, hasState(INITIAL.code()));
                    for (var entity : entities) {
                        transactionContext.execute(() -> {
                            processed.computeIfAbsent(entity.getId(), id -> new AtomicInteger()).incrementAndGet();
                            entity.transitionTerminating();
                            assertThat(store.save(entity).succeeded()).isTrue();
                        });
                    }
                } while (!entities.isEmpty());
                return null;
            }).toList();

            for (var future : executor.invokeAll(replicas, 120, TimeUnit.SECONDS)) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(processed).containsOnlyKeys(ids);
        assertThat(processed.values()).allMatch(count -> count.get() == 1);
        assertThat(ids).allMatch(id -> leaseUtil.getLeasedBy(id) == null);
    }

    @Test
    void nextNotLeased_shouldSkipRowsLockedByAnotherReplica_withoutBlocking() throws Exception {
        var first = createStore("replica-1");
        var second = createStore("replica-2");
        IntStream.range(0, 10).mapToObj(i -> createTransferProcess(UUID.randomUUID().toString(), INITIAL)).forEach(first::save);

        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();

        try {
            var firstBatch = executor.submit(() -> transactionContext.execute(() -> {
                var entities = first.nextNotLeased(5, hasState(INITIAL.code()));
                locked.countDown();
                awaitUninterruptibly(release);
                return entities;
            }));

            assertThat(locked.await(30, TimeUnit.SECONDS)).isTrue();

            // the first transaction is still open: its rows are locked and its leases are not committed yet
            var secondBatch = transactionContext.execute(() -> second.nextNotLeased(10, hasState(INITIAL.code())));

            release.countDown();
            var firstIds = firstBatch.get(30, TimeUnit.SECONDS).stream().map(TransferProcess::getId).toList();
            var secondIds = secondBatch.stream().map(TransferProcess::getId).toList();

            assertThat(firstIds).hasSize(5);
            assertThat(secondIds).hasSize(5).doesNotContainAnyElementsOf(firstIds);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void save_shouldFailWithAlreadyLeased_whenEntityIsLeasedByAnotherReplica() throws Exception {
        var first = createStore("replica-1");
        var second = createStore("replica-2");
        var entity = createTransferProcess(UUID.randomUUID().toString(), INITIAL);
        first.save(entity);

        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var firstBatch = executor.submit(() -> transactionContext.execute(() -> {
                var entities = first.nextNotLeased(1, hasState(INITIAL.code()));
                locked.countDown();
                awaitUninterruptibly(release);
                return entities;
            }));

            assertThat(locked.await(30, TimeUnit.SECONDS)).isTrue();

            // replica-2 updates the entity while replica-1 holds the row lock and is about to commit its lease
            var stale = second.findById(entity.getId());
            stale.transitionTerminating();
            var secondSave = executor.submit(() -> transactionContext.execute(() -> second.save(stale)));

            release.countDown();
            assertThat(firstBatch.get(30, TimeUnit.SECONDS)).hasSize(1);

            var result = secondSave.get(30, TimeUnit.SECONDS);
            assertThat(result.failed()).isTrue();
            assertThat(result.reason()).isEqualTo(ALREADY_LEASED);
            assertThat(second.findById(entity.getId()).getState()).isEqualTo(INITIAL.code());
            assertThat(leaseUtil.isLeased(entity.getId(), "replica-1")).isTrue();
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void save_shouldFailWithConflict_whenLeaseExpiredAndAnotherReplicaSaved() {
        // replica-2 runs two minutes ahead, so it sees the 60 seconds lease of replica-1 as expired
        var first = createStore("replica-1");
        var second = createStore("replica-2", Clock.offset(clock, Duration.ofMinutes(2)));
        var entity = createTransferProcess(UUID.randomUUID().toString(), INITIAL);
        first.save(entity);

        var leasedByFirst = first.nextNotLeased(1, hasState(INITIAL.code()));
        assertThat(leasedByFirst).hasSize(1);
        var leasedBySecond = second.nextNotLeased(1, hasState(INITIAL.code()));
        assertThat(leasedBySecond).hasSize(1);

        var takenOver = leasedBySecond.get(0);
        takenOver.transitionTerminating();
        assertThat(second.save(takenOver).succeeded()).isTrue();

        var late = leasedByFirst.get(0);
        late.transitionRequesting();
        var result = first.save(late);

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).isEqualTo(CONFLICT);
        assertThat(first.findById(entity.getId()).getState()).isEqualTo(TERMINATING.code());
    }

    private SqlTransferProcessStore createStore(String leaseHolder) {
        return createStore(leaseHolder, clock);
    }

    private SqlTransferProcessStore createStore(String leaseHolder, Clock storeClock) {
        return new SqlTransferProcessStore(dataSourceRegistry, extension.getDatasourceName(), transactionContext,
                typeManager::getMapper, new PostgresDialectStatements(storeClock), queryExecutor, leaseHolder, storeClock);
    }

    private Connection getConnection() {
        try {
            return dataSourceRegistry.resolve(extension.getDatasourceName()).getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void awaitUninterruptibly(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for latch");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
