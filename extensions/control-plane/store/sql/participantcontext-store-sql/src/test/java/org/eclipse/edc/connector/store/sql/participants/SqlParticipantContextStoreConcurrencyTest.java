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

package org.eclipse.edc.connector.store.sql.participants;

import org.eclipse.edc.connector.store.sql.participantcontext.ParticipantContextStoreStatements;
import org.eclipse.edc.connector.store.sql.participantcontext.SqlParticipantContextStore;
import org.eclipse.edc.connector.store.sql.participantcontext.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.transaction.local.LocalDataSourceRegistry;
import org.eclipse.edc.transaction.local.LocalTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link SqlParticipantContextStore#findByIdForUpdate} locks the participant context until the enclosing
 * transaction completes.
 * <p>
 * This cannot live in {@code ParticipantContextStoreTestBase}, nor reuse the transaction context of
 * {@link PostgresqlStoreSetupExtension}: that fixture wires a {@code NoopTransactionContext} over a
 * {@code DefaultDataSourceRegistry}, so every connection auto-commits and the {@code SELECT ... FOR UPDATE} row lock
 * would be released at statement end. The production pair is assembled here instead.
 */
@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlParticipantContextStoreConcurrencyTest {

    private static final String PARTICIPANT_CONTEXT_ID = "participant1";
    private static final int THREADS = 8;
    private static final int UPDATES_PER_THREAD = 25;

    private final ParticipantContextStoreStatements statements = new PostgresDialectStatements();

    private LocalTransactionContext transactionContext;
    private SqlParticipantContextStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        transactionContext = new LocalTransactionContext(new ConsoleMonitor());
        var registry = new LocalDataSourceRegistry(transactionContext);
        // DefaultDataSourceRegistry.resolve() hands back the raw DataSource, which the local registry then enlists
        registry.register(extension.getDatasourceName(), extension.getDataSourceRegistry().resolve(extension.getDatasourceName()));

        store = new SqlParticipantContextStore(registry, extension.getDatasourceName(), transactionContext,
                new JacksonTypeManager()::getMapper, queryExecutor, statements);

        extension.runQuery(TestUtils.getResourceFileContentAsString("participant-context-schema.sql"));

        transactionContext.execute(() -> store.create(ParticipantContext.Builder.newInstance()
                .id(PARTICIPANT_CONTEXT_ID)
                .identity("did:web:" + PARTICIPANT_CONTEXT_ID)
                .build()));
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getParticipantContextTable() + " CASCADE");
    }

    @Test
    void findByIdForUpdate_concurrentReadModifyWrite_shouldNotLoseUpdates() throws Exception {
        var barrier = new CyclicBarrier(THREADS);
        var executor = Executors.newFixedThreadPool(THREADS);

        try {
            var tasks = IntStream.range(0, THREADS).mapToObj(thread -> (Callable<Void>) () -> {
                barrier.await(30, TimeUnit.SECONDS);
                for (var i = 0; i < UPDATES_PER_THREAD; i++) {
                    var key = "t%d-%d".formatted(thread, i);
                    transactionContext.execute(() -> {
                        var participantContext = store.findByIdForUpdate(PARTICIPANT_CONTEXT_ID).getContent();
                        var properties = new HashMap<>(participantContext.getProperties());
                        properties.put(key, "value");
                        store.update(ParticipantContext.Builder.newInstance()
                                .id(participantContext.getId())
                                .identity(participantContext.getIdentity())
                                .createdAt(participantContext.getCreatedAt())
                                .properties(properties)
                                .build());
                    });
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
                .flatMap(t -> IntStream.range(0, UPDATES_PER_THREAD).mapToObj(i -> "t%d-%d".formatted(t, i)))
                .toArray(String[]::new);

        assertThat(transactionContext.execute(() -> store.findById(PARTICIPANT_CONTEXT_ID).getContent().getProperties()))
                .hasSize(THREADS * UPDATES_PER_THREAD)
                .containsKeys(expectedKeys);
    }

    @Test
    void findByIdForUpdate_shouldBlockConcurrentLockedRead_untilTransactionCompletes() throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var secondCompleted = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            executor.submit(() -> transactionContext.execute(() -> {
                store.findByIdForUpdate(PARTICIPANT_CONTEXT_ID);
                locked.countDown();
                awaitUninterruptibly(release);
            }));

            assertThat(locked.await(30, TimeUnit.SECONDS)).isTrue();

            executor.submit(() -> {
                transactionContext.execute(() -> store.findByIdForUpdate(PARTICIPANT_CONTEXT_ID));
                secondCompleted.countDown();
            });

            // the second read must be waiting on the row lock held by the still-open first transaction
            assertThat(secondCompleted.await(1, TimeUnit.SECONDS)).isFalse();

            release.countDown();
            assertThat(secondCompleted.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void findById_shouldNotBlock_whenLocked() throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();

        try {
            executor.submit(() -> transactionContext.execute(() -> {
                store.findByIdForUpdate(PARTICIPANT_CONTEXT_ID);
                locked.countDown();
                awaitUninterruptibly(release);
            }));

            assertThat(locked.await(30, TimeUnit.SECONDS)).isTrue();

            assertThat(transactionContext.execute(() -> store.findById(PARTICIPANT_CONTEXT_ID)).succeeded()).isTrue();
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
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
