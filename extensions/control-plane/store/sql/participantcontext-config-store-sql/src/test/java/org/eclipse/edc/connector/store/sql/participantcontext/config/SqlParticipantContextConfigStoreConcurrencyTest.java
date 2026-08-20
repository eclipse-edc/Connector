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

package org.eclipse.edc.connector.store.sql.participantcontext.config;

import org.eclipse.edc.connector.store.sql.participantcontext.config.schema.postgres.PostgresDialectStatementsConfig;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.transaction.local.LocalDataSourceRegistry;
import org.eclipse.edc.transaction.local.LocalTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that concurrent {@link SqlParticipantContextConfigStore#merge} invocations do not lose entries.
 * <p>
 * This cannot live in {@code ParticipantContextConfigStoreTestBase}, nor reuse the transaction context of
 * {@link PostgresqlStoreSetupExtension}: that fixture wires a {@code NoopTransactionContext} over a
 * {@code DefaultDataSourceRegistry}, so every connection auto-commits and the {@code SELECT ... FOR UPDATE} row lock
 * would be released at statement end. The production pair is assembled here instead.
 */
@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlParticipantContextConfigStoreConcurrencyTest {

    private static final String PARTICIPANT_CONTEXT_ID = "participant1";
    private static final int THREADS = 8;
    private static final int MERGES_PER_THREAD = 50;

    private final ParticipantContextConfigStoreStatements statements = new PostgresDialectStatementsConfig();

    private LocalTransactionContext transactionContext;
    private SqlParticipantContextConfigStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        transactionContext = new LocalTransactionContext(new ConsoleMonitor());
        var registry = new LocalDataSourceRegistry(transactionContext);
        // DefaultDataSourceRegistry.resolve() hands back the raw DataSource, which the local registry then enlists
        registry.register(extension.getDatasourceName(), extension.getDataSourceRegistry().resolve(extension.getDatasourceName()));

        store = new SqlParticipantContextConfigStore(registry, extension.getDatasourceName(), transactionContext,
                new JacksonTypeManager().getMapper(), queryExecutor, statements);

        extension.runQuery(TestUtils.getResourceFileContentAsString("participant-context-config-schema.sql"));
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getParticipantContextConfigTable() + " CASCADE");
    }

    @Test
    void merge_concurrently_shouldNotLoseEntries() throws Exception {
        // a barrier placed between the read and the write would deadlock the fixed implementation, since the losing
        // thread blocks inside the SELECT ... FOR UPDATE. Synchronise the start instead and rely on volume.
        var barrier = new CyclicBarrier(THREADS);
        var executor = Executors.newFixedThreadPool(THREADS);

        try {
            var tasks = IntStream.range(0, THREADS).mapToObj(thread -> (Callable<Void>) () -> {
                barrier.await(30, TimeUnit.SECONDS);
                for (var i = 0; i < MERGES_PER_THREAD; i++) {
                    var key = "t%d-%d".formatted(thread, i);
                    transactionContext.execute(() -> store.merge(patch(Map.of(key, "value"))));
                }
                return null;
            }).toList();

            for (var future : executor.invokeAll(tasks, 120, TimeUnit.SECONDS)) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        var expectedKeys = IntStream.range(0, THREADS)
                .boxed()
                .flatMap(t -> IntStream.range(0, MERGES_PER_THREAD).mapToObj(i -> "t%d-%d".formatted(t, i)))
                .toList();

        assertThat(transactionContext.execute(() -> store.get(PARTICIPANT_CONTEXT_ID)))
                .isNotNull()
                .satisfies(cfg -> assertThat(cfg.getEntries())
                        .hasSize(THREADS * MERGES_PER_THREAD)
                        .containsKeys(expectedKeys.toArray(String[]::new)));
    }

    @Test
    void merge_shouldBlockConcurrentMerge_untilTransactionCompletes() throws Exception {
        var merged = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var secondCompleted = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            executor.submit(() -> transactionContext.execute(() -> {
                store.merge(patch(Map.of("first", "value")));
                merged.countDown();
                awaitUninterruptibly(release);
            }));

            assertThat(merged.await(30, TimeUnit.SECONDS)).isTrue();

            executor.submit(() -> {
                transactionContext.execute(() -> store.merge(patch(Map.of("second", "value"))));
                secondCompleted.countDown();
            });

            // the second merge must be waiting on the row lock held by the still-open first transaction
            assertThat(secondCompleted.await(1, TimeUnit.SECONDS)).isFalse();

            release.countDown();
            assertThat(secondCompleted.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(transactionContext.execute(() -> store.get(PARTICIPANT_CONTEXT_ID)))
                .isNotNull()
                .satisfies(cfg -> assertThat(cfg.getEntries()).containsOnlyKeys("first", "second"));
    }

    private ParticipantContextConfiguration patch(Map<String, String> entries) {
        return ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .entries(entries)
                .build();
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
