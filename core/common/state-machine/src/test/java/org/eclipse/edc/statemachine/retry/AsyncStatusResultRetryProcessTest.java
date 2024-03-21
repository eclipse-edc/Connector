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
import org.eclipse.edc.spi.response.ResponseFailure;
import org.eclipse.edc.spi.response.StatusResult;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncStatusResultRetryProcessTest {

    private final BiConsumer<TestEntity, StatusResult<String>> onSuccess = mock();
    private final BiConsumer<TestEntity, String> onSuccessResult = mock();
    private final BiConsumer<TestEntity, Throwable> onRetryExhausted = mock();
    private final BiConsumer<TestEntity, ResponseFailure> onFatalError = mock();
    private final BiConsumer<TestEntity, Throwable> onFailure = mock();
    private final int retryLimit = 2;
    private final int millis = 123;
    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(millis), UTC);
    private final EntityRetryProcessConfiguration configuration = new EntityRetryProcessConfiguration(retryLimit, () -> () -> 1L);
    private final Supplier<CompletableFuture<StatusResult<String>>> process = mock();

    @Test
    void shouldExecuteOnSuccess() {
        when(process.get()).thenReturn(CompletableFuture.completedFuture(StatusResult.success("content")));
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).build();
        var retryProcess = new AsyncStatusResultRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        var result = retryProcess.onSuccess(onSuccess).execute("any");

        assertThat(result).isTrue();
        verify(process).get();
        verify(onSuccess).accept(eq(entity), argThat(it -> it.succeeded() && it.getContent().equals("content")));
    }

    @Test
    void shouldExecuteOnSuccessResult() {
        when(process.get()).thenReturn(CompletableFuture.completedFuture(StatusResult.success("content")));
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).build();
        var retryProcess = new AsyncStatusResultRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        var result = retryProcess.onSuccessResult(onSuccessResult).execute("any");

        assertThat(result).isTrue();
        verify(process).get();
        verify(onSuccessResult).accept(eq(entity), argThat(it -> it.equals("content")));
    }

    @Test
    void shouldReloadEntityIfConfigured() {
        when(process.get()).thenReturn(CompletableFuture.completedFuture(StatusResult.success("content")));
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).build();
        var retryProcess = new AsyncStatusResultRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);
        var reloadedEntity = TestEntity.Builder.newInstance().id(entity.getId()).clock(clock).state(10).build();

        var result = retryProcess.onSuccess(onSuccess).entityRetrieve(id -> reloadedEntity).execute("any");

        assertThat(result).isTrue();
        verify(process).get();
        verify(onSuccess).accept(eq(reloadedEntity), argThat(it -> it.succeeded() && it.getContent().equals("content")));
    }

    @Test
    void shouldExecuteOnFatalError() {
        CompletableFuture<StatusResult<String>> statusResult = CompletableFuture.completedFuture(StatusResult.failure(FATAL_ERROR));
        when(process.get()).thenReturn(statusResult);
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).stateCount(retryLimit + 1).stateTimestamp(millis - 2L).build();
        var retryProcess = new AsyncStatusResultRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        retryProcess.onSuccess((e, r) -> {}).onFatalError(onFatalError).execute("any");

        verify(onFatalError).accept(entity, statusResult.join().getFailure());
    }

    @Test
    void shouldExecuteOnRetryExhausted_whenFailureAndRetriesHaveBeenExhausted() {
        CompletableFuture<StatusResult<String>> statusResult = CompletableFuture.failedFuture(new EdcException("error"));
        when(process.get()).thenReturn(statusResult);
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).stateCount(retryLimit + 1).stateTimestamp(millis - 2L).build();
        var retryProcess = new AsyncStatusResultRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        retryProcess.onSuccess((e, r) -> {}).onRetryExhausted(onRetryExhausted).execute("any");

        verify(onRetryExhausted).accept(eq(entity), isA(EdcException.class));
    }

    @Test
    void shouldExecuteOnRetry_whenFailureAndRetriesHaveNotBeenExhausted() {
        CompletableFuture<StatusResult<String>> statusResult = CompletableFuture.failedFuture(new EdcException("error"));
        when(process.get()).thenReturn(statusResult);
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).stateCount(retryLimit).stateTimestamp(millis - 2L).build();
        var retryProcess = new AsyncStatusResultRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        retryProcess.onSuccess((e, r) -> {}).onFailure(onFailure).execute("any");

        verify(onFailure).accept(eq(entity), isA(EdcException.class));
    }
}
