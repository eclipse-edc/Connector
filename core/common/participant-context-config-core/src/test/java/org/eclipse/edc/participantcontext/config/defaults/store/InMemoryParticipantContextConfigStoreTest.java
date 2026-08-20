/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.participantcontext.config.defaults.store;

import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStoreTestBase;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryParticipantContextConfigStoreTest extends ParticipantContextConfigStoreTestBase {

    private static final int THREADS = 8;
    private static final int MERGES_PER_THREAD = 50;

    private final InMemoryParticipantContextConfigStore store = new InMemoryParticipantContextConfigStore();

    @Override
    protected ParticipantContextConfigStore getStore() {
        return store;
    }

    @Test
    void merge_concurrently_shouldNotLoseEntries() throws Exception {
        var barrier = new CyclicBarrier(THREADS);
        var executor = Executors.newFixedThreadPool(THREADS);

        try {
            var tasks = IntStream.range(0, THREADS).mapToObj(thread -> (Callable<Void>) () -> {
                barrier.await(30, TimeUnit.SECONDS);
                for (var i = 0; i < MERGES_PER_THREAD; i++) {
                    store.merge(ParticipantContextConfiguration.Builder.newInstance()
                            .participantContextId("participant1")
                            .entries(Map.of("t%d-%d".formatted(thread, i), "value"))
                            .build());
                }
                return null;
            }).toList();

            for (var future : executor.invokeAll(tasks, 60, TimeUnit.SECONDS)) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(store.get("participant1")).isNotNull()
                .satisfies(cfg -> assertThat(cfg.getEntries()).hasSize(THREADS * MERGES_PER_THREAD));
    }
}
