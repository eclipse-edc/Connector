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

package org.eclipse.edc.connector.policy.monitor.store.sql;

import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntry;
import org.eclipse.edc.connector.policy.monitor.store.sql.schema.PolicyMonitorStatements;
import org.eclipse.edc.connector.policy.monitor.store.sql.schema.PostgresPolicyMonitorStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.transaction.local.LocalDataSourceRegistry;
import org.eclipse.edc.transaction.local.LocalTransactionContext;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntryStates.STARTED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * Simulates multiple runtime replicas running the policy monitor watchdog against the same database.
 * <p>
 * It uses the production {@link LocalTransactionContext} instead of the {@code NoopTransactionContext} of
 * {@link PostgresqlStoreSetupExtension}, so that the {@code FOR UPDATE SKIP LOCKED} row locks taken by
 * {@link SqlPolicyMonitorStore#nextNotLeased} are held until the enclosing transaction completes.
 */
@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlPolicyMonitorStoreConcurrencyTest {

    private static final int REPLICAS = 5;
    private static final int ENTRIES = 300;
    private static final int BATCH_SIZE = 20;

    private final PolicyMonitorStatements statements = new PostgresPolicyMonitorStatements();
    private final Clock clock = Clock.systemUTC();
    private LocalTransactionContext transactionContext;
    private LocalDataSourceRegistry dataSourceRegistry;
    private PostgresqlStoreSetupExtension extension;
    private QueryExecutor queryExecutor;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        this.extension = extension;
        this.queryExecutor = queryExecutor;
        transactionContext = new LocalTransactionContext(new ConsoleMonitor());
        dataSourceRegistry = new LocalDataSourceRegistry(transactionContext);
        // DefaultDataSourceRegistry.resolve() hands back the raw DataSource, which the local registry then enlists
        dataSourceRegistry.register(extension.getDatasourceName(), extension.getDataSourceRegistry().resolve(extension.getDatasourceName()));

        extension.runQuery(TestUtils.getResourceFileContentAsString("policy-monitor-schema.sql"));
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getPolicyMonitorTable() + " CASCADE");
    }

    @Test
    void multipleReplicas_shouldProcessEachEntryExactlyOnce() throws Exception {
        var seedStore = createStore("seed");
        var ids = IntStream.range(0, ENTRIES).mapToObj(i -> createEntry()).peek(seedStore::save).map(PolicyMonitorEntry::getId).toList();
        Thread.sleep(5);
        // same as PolicyMonitorManagerImpl.notYetProcessed(): only entries not updated since the run started are due
        var notYetProcessed = criterion("updatedAt", "<", clock.millis());

        var processed = new ConcurrentHashMap<String, AtomicInteger>();
        var processedBy = new ConcurrentHashMap<String, Integer>();
        var barrier = new CyclicBarrier(REPLICAS);
        var executor = Executors.newFixedThreadPool(REPLICAS);

        try {
            var replicas = IntStream.range(0, REPLICAS).mapToObj(replica -> (Callable<Void>) () -> {
                var store = createStore("replica-" + replica);
                barrier.await(30, TimeUnit.SECONDS);
                runWatchdog(store, notYetProcessed, entry -> {
                    processed.computeIfAbsent(entry.getId(), id -> new AtomicInteger()).incrementAndGet();
                    processedBy.merge("replica-" + replica, 1, Integer::sum);
                });
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
        assertThat(processedBy.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(ENTRIES);
        assertThat(ids).allMatch(id -> seedStore.findById(id) != null && leasedBy(id) == null);
    }

    @Test
    void nextNotLeased_shouldSkipRowsLockedByAnotherReplica_withoutBlocking() throws Exception {
        var first = createStore("replica-1");
        var second = createStore("replica-2");
        IntStream.range(0, 10).mapToObj(i -> createEntry()).forEach(first::save);

        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();

        try {
            var firstBatch = executor.submit(() -> transactionContext.execute(() -> {
                var entries = first.nextNotLeased(5, hasState(STARTED.code()));
                locked.countDown();
                awaitUninterruptibly(release);
                return entries;
            }));

            assertThat(locked.await(30, TimeUnit.SECONDS)).isTrue();

            // the first transaction is still open: its rows are locked and its leases are not committed yet
            var secondBatch = transactionContext.execute(() -> second.nextNotLeased(10, hasState(STARTED.code())));

            release.countDown();
            var firstIds = firstBatch.get(30, TimeUnit.SECONDS).stream().map(PolicyMonitorEntry::getId).toList();
            var secondIds = secondBatch.stream().map(PolicyMonitorEntry::getId).toList();

            assertThat(firstIds).hasSize(5);
            assertThat(secondIds).hasSize(5).doesNotContainAnyElementsOf(firstIds);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
    }

    /**
     * Mirrors PolicyMonitorManagerImpl.checkPolicies() and PolicyMonitor.monitor(): fetch batches until a partial one
     * is returned, then process and save every entry in its own transaction.
     */
    private void runWatchdog(SqlPolicyMonitorStore store, Criterion notYetProcessed, Consumer<PolicyMonitorEntry> processor) {
        List<PolicyMonitorEntry> entries;
        do {
            entries = store.nextNotLeased(BATCH_SIZE, hasState(STARTED.code()), notYetProcessed);
            entries.forEach(entry -> transactionContext.execute(() -> {
                processor.accept(entry);
                entry.setModified();
                entry.updateStateTimestamp();
                store.save(entry);
            }));
        } while (entries.size() >= BATCH_SIZE);
    }

    private SqlPolicyMonitorStore createStore(String leaseHolder) {
        return new SqlPolicyMonitorStore(dataSourceRegistry, extension.getDatasourceName(), transactionContext,
                statements, new JacksonTypeManager()::getMapper, queryExecutor, leaseHolder, clock);
    }

    private PolicyMonitorEntry createEntry() {
        return PolicyMonitorEntry.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractId(UUID.randomUUID().toString())
                .state(STARTED.code())
                .build();
    }

    private @Nullable String leasedBy(String entityId) {
        var sql = "SELECT %s FROM %s WHERE %s = ?".formatted(statements.getLeasedByColumn(), statements.getPolicyMonitorTable(), statements.getIdColumn());
        return transactionContext.execute(() -> {
            try (var connection = dataSourceRegistry.resolve(extension.getDatasourceName()).getConnection()) {
                return queryExecutor.single(connection, false, rs -> Optional.ofNullable(rs.getString(1)), sql, entityId).orElse(null);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
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
