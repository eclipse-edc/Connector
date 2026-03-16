/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.statemachine;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.statemachine.retry.TestEntity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessorImplTest {

    private static final long DELAY = 2L;
    private final long millis = 123;
    private final WaitStrategy fixedWaitStrategy = () -> DELAY;
    private final long shouldDelayTime = millis - DELAY + 1;
    private final long shouldNotDelayTime = millis - DELAY;
    private final int retryLimit = 2;
    private final Monitor monitor = mock();
    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(millis), UTC);
    private final EntityRetryProcessConfiguration configuration = new EntityRetryProcessConfiguration(retryLimit, () -> fixedWaitStrategy);

    @Test
    void shouldReturnTheProcessedCount() {
        var entity = TestEntity.Builder.newInstance().id("id").build();
        var processor = ProcessorImpl.Builder.newInstance(() -> List.of(entity), configuration, clock, monitor)
                .process(e -> CompletableFuture.completedFuture(StatusResult.success()))
                .build();

        var count = processor.process();

        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldProcessAndLog_whenItShouldRetryButNotDelay() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateTimestamp(shouldNotDelayTime)
                .stateCount(2).build();

        var processor = ProcessorImpl.Builder.newInstance(() -> List.of(entity), configuration, clock, monitor)
                .process(e -> CompletableFuture.completedFuture(StatusResult.success()))
                .build();

        var count = processor.process();

        assertThat(count).isEqualTo(1);
        verify(monitor).debug(contains("retry #1 of 2"));
    }

    @Test
    void shouldNotProcessAndCallOnNotProcessed_whenItShouldDelay() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateTimestamp(shouldDelayTime).stateCount(2).build();
        Consumer<TestEntity> onNotProcessed = mock();

        var processor = ProcessorImpl.Builder.newInstance(() -> List.of(entity), configuration, clock, monitor)
                .process(e -> CompletableFuture.completedFuture(StatusResult.success()))
                .onNotProcessed(onNotProcessed)
                .build();

        var count = processor.process();

        assertThat(count).isEqualTo(0);
        verify(monitor).debug(contains("not be attempted before"));
        verify(onNotProcessed).accept(entity);
    }

    @Test
    void shouldExecuteGuard_whenItsPredicateMatches() {
        var entity = TestEntity.Builder.newInstance().id("id").build();
        Function<TestEntity, CompletableFuture<StatusResult<Void>>> process = mock();
        Function<TestEntity, CompletableFuture<StatusResult<Void>>> guardProcess = mock();
        when(guardProcess.apply(any())).thenReturn(CompletableFuture.completedFuture(StatusResult.success()));
        var processor = ProcessorImpl.Builder.newInstance(() -> List.of(entity), configuration, clock, monitor)
                .guard(e -> true, guardProcess)
                .process(process)
                .build();

        var count = processor.process();

        assertThat(count).isEqualTo(1);
        verify(guardProcess).apply(entity);
        verifyNoInteractions(process);
    }

    @Test
    void shouldExecuteDefaultProcessor_whenGuardPredicateDoesNotMatch() {
        var entity = TestEntity.Builder.newInstance().id("id").build();
        Function<TestEntity, CompletableFuture<StatusResult<Void>>> process = mock();
        Function<TestEntity, CompletableFuture<StatusResult<Void>>> guardProcess = mock();
        when(process.apply(any())).thenReturn(CompletableFuture.completedFuture(StatusResult.success()));
        var processor = ProcessorImpl.Builder.newInstance(() -> List.of(entity), configuration, clock, monitor)
                .guard(e -> false, guardProcess)
                .process(process)
                .build();

        var count = processor.process();

        assertThat(count).isEqualTo(1);
        verify(process).apply(entity);
        verifyNoInteractions(guardProcess);
    }

    @Test
    void shouldNotExecuteOnNotProcessed_whenEntityProcessed() {
        var entity = TestEntity.Builder.newInstance().id("id").build();
        Consumer<TestEntity> onNotProcessed = mock();
        var processor = ProcessorImpl.Builder.newInstance(() -> List.of(entity), configuration, clock, monitor)
                .process(e -> CompletableFuture.completedFuture(StatusResult.success()))
                .onNotProcessed(onNotProcessed)
                .build();

        processor.process();

        verifyNoInteractions(onNotProcessed);
    }

}
