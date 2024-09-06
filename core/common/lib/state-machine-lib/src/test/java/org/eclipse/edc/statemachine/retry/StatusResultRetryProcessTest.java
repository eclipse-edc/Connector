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
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StatusResultRetryProcessTest {

    private final BiConsumer<TestEntity, String> onSuccess = mock(BiConsumer.class);
    private final BiConsumer<TestEntity, ResponseFailure> onFatalError = mock(BiConsumer.class);
    private final BiConsumer<TestEntity, ResponseFailure> onRetryExhausted = mock(BiConsumer.class);
    private final BiConsumer<TestEntity, ResponseFailure> onFailure = mock(BiConsumer.class);
    private final int retryLimit = 2;
    private final int millis = 123;
    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(millis), UTC);
    private final EntityRetryProcessConfiguration configuration = new EntityRetryProcessConfiguration(retryLimit, () -> () -> 1L);
    private final Supplier<StatusResult<String>> process = mock(Supplier.class);

    @Test
    void shouldExecuteOnSuccess() {
        when(process.get()).thenReturn(StatusResult.success("content"));
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).build();
        var retryProcess = new StatusResultRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        var result = retryProcess.onSuccess(onSuccess).execute("any");

        assertThat(result).isTrue();
        verify(process).get();
        verify(onSuccess).accept(entity, "content");
    }

    @Test
    void shouldExecuteOnFatalError() {
        StatusResult<String> statusResult = StatusResult.failure(FATAL_ERROR, "error");
        when(process.get()).thenReturn(statusResult);
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).build();
        var retryProcess = new StatusResultRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        retryProcess.onFatalError(onFatalError).execute("any");

        verify(onFatalError).accept(entity, statusResult.getFailure());
    }

    @Test
    void shouldExecuteOnRetryExhausted_whenFailureAndRetriesHaveBeenExhausted() {
        StatusResult<String> statusResult = StatusResult.failure(ERROR_RETRY, "error");
        when(process.get()).thenReturn(statusResult);
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).stateCount(retryLimit + 1).stateTimestamp(millis - 2L).build();
        var retryProcess = new StatusResultRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        retryProcess.onRetryExhausted(onRetryExhausted).execute("any");

        verify(onRetryExhausted).accept(entity, statusResult.getFailure());
    }

    @Test
    void shouldExecuteOnRetry_whenFailureAndRetriesHaveNotBeenExhausted() {
        StatusResult<String> statusResult = StatusResult.failure(ERROR_RETRY, "error");
        when(process.get()).thenReturn(statusResult);
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).stateCount(retryLimit).stateTimestamp(millis - 2L).build();
        var retryProcess = new StatusResultRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        retryProcess.onFailure(onFailure).execute("any");

        verify(onFailure).accept(entity, statusResult.getFailure());
    }


    @Test
    void shouldCallFatalError_whenExceptionIsThrown() {
        when(process.get()).thenThrow(new EdcException("code throws an exception"));
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).clock(clock).build();
        var retryProcess = new StatusResultRetryProcess<>(entity, process, mock(Monitor.class), clock, configuration);

        var result = retryProcess.onSuccess(onSuccess).onFatalError(onFatalError).execute("any");

        assertThat(result).isTrue();
        verify(process).get();
        verify(onFatalError).accept(same(entity), any());
        verifyNoInteractions(onSuccess);
    }
}
