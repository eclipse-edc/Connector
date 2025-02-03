/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.statemachine.retry.processor;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.statemachine.retry.TestEntity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static java.time.ZoneOffset.UTC;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.statemachine.retry.processor.Process.future;
import static org.eclipse.edc.statemachine.retry.processor.Process.result;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RetryProcessorTest {

    private static final long DELAY = 2L;
    private final long millis = 123;
    private final Monitor monitor = mock();
    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(millis), UTC);
    private final long shouldDelayTime = millis - DELAY + 1;
    private final long shouldNotDelayTime = millis - DELAY;
    private final WaitStrategy fixedWaitStrategy = () -> DELAY;
    private final int retryLimit = 2;
    private final EntityRetryProcessConfiguration configuration = new EntityRetryProcessConfiguration(retryLimit, () -> fixedWaitStrategy);

    private final BiConsumer<TestEntity, String> success = mock();
    private final BiConsumer<TestEntity, Throwable> failure = mock();
    private final BiConsumer<TestEntity, Throwable> finalFailure = mock();

    @Test
    void shouldExecuteAllTheStagesInTheRightOrder_whenItIsRetryButDoesNotDelay() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateTimestamp(shouldDelayTime).stateCount(1).build();
        BiFunction<TestEntity, Object, CompletableFuture<Integer>> firstProcess = mock();
        when(firstProcess.apply(any(), any())).thenReturn(CompletableFuture.completedFuture(1));
        BiFunction<TestEntity, Integer, StatusResult<String>> secondProcess = mock();
        when(secondProcess.apply(any(), any())).thenAnswer(i -> StatusResult.success(i.getArgument(1) + " second"));

        var processed = new RetryProcessor<>(entity, monitor, clock, configuration)
                .doProcess(future("async process", firstProcess))
                .doProcess(result("sync process", secondProcess))
                .onSuccess(success)
                .onFailure(failure)
                .onFinalFailure(finalFailure)
                .execute();

        assertThat(processed).isTrue();
        verify(success).accept(entity, "1 second");
        verifyNoInteractions(failure, finalFailure);
        var inOrder = inOrder(firstProcess, secondProcess);
        inOrder.verify(firstProcess).apply(any(), any());
        inOrder.verify(secondProcess).apply(any(), any());
    }

    @Test
    void shouldNotProcess_whenItShouldDelay() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateTimestamp(shouldDelayTime).stateCount(2).build();
        BiFunction<TestEntity, Object, StatusResult<String>> process = mock();

        var processed = new RetryProcessor<>(entity, monitor, clock, configuration)
                .doProcess(result("mock", process))
                .onSuccess(success).onFailure(failure).onFinalFailure(finalFailure)
                .execute();

        assertThat(processed).isFalse();
        verify(monitor).debug(contains("not be attempted before"));
        verifyNoInteractions(process, success, failure, finalFailure);
    }

    @Test
    void shouldNotProcessSecond_whenFirstFails() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateTimestamp(shouldDelayTime).stateCount(1).build();
        BiFunction<TestEntity, Object, StatusResult<String>> first = mock();
        when(first.apply(any(), any())).thenReturn(StatusResult.failure(ERROR_RETRY));
        BiFunction<TestEntity, String, StatusResult<String>> second = mock();

        var processed = new RetryProcessor<>(entity, monitor, clock, configuration)
                .doProcess(result("first", first))
                .doProcess(result("second", second))
                .onSuccess(success)
                .onFailure(failure)
                .onFinalFailure(finalFailure)
                .execute();

        assertThat(processed).isTrue();
        verify(failure).accept(same(entity), isA(Throwable.class));
        verifyNoInteractions(second, success, finalFailure);
    }

    @Test
    void shouldInvokeFailureHandler_whenFailureHappensAndRetryLimitNotExceeded() {
        var entityId = UUID.randomUUID().toString();
        var entity = TestEntity.Builder.newInstance().id(entityId).stateTimestamp(shouldNotDelayTime).build();

        org.eclipse.edc.statemachine.retry.processor.Process<TestEntity, Object, String> process = context -> failedFuture(new EntityStateException(
                        TestEntity.Builder.newInstance().id(entityId).stateCount(retryLimit).build(), "process", "generic error"));

        var processed = new RetryProcessor<>(entity, monitor, clock, configuration)
                .doProcess(process)
                .onSuccess(success)
                .onFailure(failure)
                .onFinalFailure(finalFailure)
                .execute();

        assertThat(processed).isTrue();
        verify(failure).accept(same(entity), isA(Throwable.class));
        verify(monitor).debug(contains("failed to process. Cause: generic error"));
        verifyNoInteractions(success, finalFailure);
    }

    @Test
    void shouldInvokeFinalFailureHandler_whenRetryExhausted() {
        var entityId = UUID.randomUUID().toString();
        var entity = TestEntity.Builder.newInstance().id(entityId).stateTimestamp(shouldNotDelayTime).build();

        org.eclipse.edc.statemachine.retry.processor.Process<TestEntity, Object, String> process = context -> failedFuture(new EntityStateException(
                TestEntity.Builder.newInstance().id(entityId).stateCount(retryLimit + 1).build(), "process", "generic error"));

        var processed = new RetryProcessor<>(entity, monitor, clock, configuration)
                .doProcess(process)
                .onSuccess(success)
                .onFailure(failure)
                .onFinalFailure(finalFailure)
                .execute();

        assertThat(processed).isTrue();
        verify(monitor).severe(contains("failed to process. Retry limit exceeded. Cause: generic error"));
        verify(finalFailure).accept(same(entity), isA(Throwable.class));
        verifyNoInteractions(success, failure);
    }

    @Test
    void shouldInvokeFinalFailureHandler_whenUnrecoverableException() {
        var entityId = UUID.randomUUID().toString();
        var entity = TestEntity.Builder.newInstance().id(entityId).stateTimestamp(shouldNotDelayTime).build();

        org.eclipse.edc.statemachine.retry.processor.Process<TestEntity, Object, String> process = context -> failedFuture(new UnrecoverableEntityStateException(
                TestEntity.Builder.newInstance().id(entityId).build(), "process", "fatal error"));

        var processed = new RetryProcessor<>(entity, monitor, clock, configuration)
                .doProcess(process)
                .onSuccess(success)
                .onFailure(failure)
                .onFinalFailure(finalFailure)
                .execute();

        assertThat(processed).isTrue();
        verify(monitor).severe(contains("failed to process. Fatal error occurred. Cause: fatal error"));
        verify(finalFailure).accept(same(entity), isA(Throwable.class));
        verifyNoInteractions(success, failure);
    }

    @Test
    void shouldInvokeFinalFailureHandler_whenGenericException() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateTimestamp(shouldDelayTime).stateCount(1).build();

        var runtimeException = new RuntimeException("generic exception");
        Process<TestEntity, Object, String> process = context -> failedFuture(runtimeException);

        var processed = new RetryProcessor<>(entity, monitor, clock, configuration)
                .doProcess(process)
                .onSuccess(success)
                .onFailure(failure)
                .onFinalFailure(finalFailure)
                .execute();

        assertThat(processed).isTrue();
        verify(monitor).severe(contains("generic exception"), same(runtimeException));
        verify(finalFailure).accept(same(entity), isA(Throwable.class));
        verifyNoInteractions(success, failure);
    }

}
