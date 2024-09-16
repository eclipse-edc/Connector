/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.statemachine.retry;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompletableFutureRetryProcessTest {

    private final BiConsumer<TestEntity, String> onSuccess = mock(BiConsumer.class);
    private final BiConsumer<TestEntity, Throwable> onRetryExhausted = mock(BiConsumer.class);
    private final BiConsumer<TestEntity, Throwable> onFailure = mock(BiConsumer.class);
    private final int retryLimit = 2;
    private final int millis = 123;
    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(millis), UTC);
    private final EntityRetryProcessConfiguration configuration = new EntityRetryProcessConfiguration(retryLimit, () -> () -> 1L);
    private final Supplier<CompletableFuture<String>> process = mock(Supplier.class);

    @Test
    void shouldExecuteOnSuccess() {
        when(process.get()).thenReturn(CompletableFuture.completedFuture("content"));
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).build();
        var retryProcess = new CompletableFutureRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        var result = retryProcess.onSuccess(onSuccess).execute("any");

        assertThat(result).isTrue();
        verify(process).get();
        verify(onSuccess).accept(entity, "content");
    }

    @Test
    void shouldReloadEntityIfConfigured() {
        when(process.get()).thenReturn(CompletableFuture.completedFuture("content"));
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).build();
        var retryProcess = new CompletableFutureRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);
        var reloadedEntity = TestEntity.Builder.newInstance().id(entity.getId()).clock(clock).state(10).build();

        var result = retryProcess.onSuccess(onSuccess).entityRetrieve(id -> reloadedEntity).execute("any");

        assertThat(result).isTrue();
        verify(process).get();
        verify(onSuccess).accept(reloadedEntity, "content");
    }

    @Test
    void shouldUsePassedEntity_whenReloadEntityReturnsNull() {
        when(process.get()).thenReturn(CompletableFuture.completedFuture("content"));
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).build();
        var retryProcess = new CompletableFutureRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        var result = retryProcess.onSuccess(onSuccess).entityRetrieve(id -> null).execute("any");

        assertThat(result).isTrue();
        verify(process).get();
        verify(onSuccess).accept(entity, "content");
    }

    @Test
    void shouldExecuteOnRetryExhausted_whenFailureAndRetriesHaveBeenExhausted() {
        CompletableFuture<String> statusResult = CompletableFuture.failedFuture(new EdcException("error"));
        when(process.get()).thenReturn(statusResult);
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).stateCount(retryLimit + 1).stateTimestamp(millis - 2L).build();
        var retryProcess = new CompletableFutureRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        retryProcess.onRetryExhausted(onRetryExhausted).execute("any");

        verify(onRetryExhausted).accept(eq(entity), isA(EdcException.class));
    }

    @Test
    void shouldExecuteOnRetry_whenFailureAndRetriesHaveNotBeenExhausted() {
        CompletableFuture<String> statusResult = CompletableFuture.failedFuture(new EdcException("error"));
        when(process.get()).thenReturn(statusResult);
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).stateCount(retryLimit).stateTimestamp(millis - 2L).build();
        var retryProcess = new CompletableFutureRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        retryProcess.onFailure(onFailure).execute("any");

        verify(onFailure).accept(eq(entity), isA(EdcException.class));
    }

    @Test
    void shouldFail_whenExceptionIsThrown() {
        when(process.get()).thenThrow(new EdcException("generic error"));
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).build();
        var retryProcess = new CompletableFutureRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        var result = retryProcess.onSuccess(onSuccess).onFailure(onFailure).execute("any");

        assertThat(result).isTrue();
        verify(process).get();
        verify(onFailure).accept(eq(entity), isA(EdcException.class));
    }
}
