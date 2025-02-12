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

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.statemachine.retry.TestEntity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FutureRetryProcessTest {

    private final Duration timeout = Duration.of(1, SECONDS);

    @Test
    void shouldReturnSuccess_whenFunctionSucceeds() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        var retryProcess = new FutureRetryProcess<TestEntity, Object, String>("process", (e, i) -> completedFuture("content"));

        var future = retryProcess.execute(new ProcessContext<>(entity, "any"));

        assertThat(future).succeedsWithin(timeout).extracting(ProcessContext::content).isEqualTo("content");
    }

    @Test
    void shouldReturnFailure_whenFunctionFails() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        var retryProcess = new FutureRetryProcess<TestEntity, Object, String>("process", (e, i) -> CompletableFuture.failedFuture(new EdcException("error")));

        var future = retryProcess.execute(new ProcessContext<>(entity, "any"));

        assertThat(future).failsWithin(timeout).withThrowableOfType(ExecutionException.class)
                .extracting(Throwable::getCause).isInstanceOfSatisfying(EntityStateException.class, exception -> {
                    assertThat(exception.getEntity()).isSameAs(entity);
                    assertThat(exception.getProcessName()).isEqualTo("process");
                    assertThat(exception.getMessage()).isEqualTo("error");
                });
    }

    @Test
    void shouldReturnUnrecoverable_whenFunctionThrowsException() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        var retryProcess = new FutureRetryProcess<TestEntity, Object, String>("process", (testEntity, o) -> {
            throw new RuntimeException("unexpected exception");
        });

        var future = retryProcess.execute(new ProcessContext<>(entity, "any"));

        assertThat(future).failsWithin(timeout).withThrowableOfType(ExecutionException.class)
                .extracting(Throwable::getCause).isInstanceOfSatisfying(UnrecoverableEntityStateException.class, exception -> {
                    assertThat(exception.getEntity()).isSameAs(entity);
                    assertThat(exception.getProcessName()).isEqualTo("process");
                    assertThat(exception.getMessage()).isEqualTo("unexpected exception");
                });
    }

    @Test
    void shouldReloadEntity_whenConfigured() {
        var entityId = UUID.randomUUID().toString();
        var entity = TestEntity.Builder.newInstance().id(entityId).build();
        var reloadedEntity = TestEntity.Builder.newInstance().id(entityId).build();
        Function<String, StoreResult<TestEntity>> entityReload = mock();
        when(entityReload.apply(any())).thenReturn(StoreResult.success(reloadedEntity));
        var retryProcess = new FutureRetryProcess<TestEntity, Object, String>("process", (e, i) -> completedFuture("content"))
                .entityReload(entityReload);

        var future = retryProcess.execute(new ProcessContext<>(entity, "any"));

        assertThat(future).succeedsWithin(timeout).extracting(ProcessContext::entity).isSameAs(reloadedEntity);
        verify(entityReload).apply(entityId);
    }

    @Test
    void shouldReturnUnrecoverable_whenEntityReloadFailed() {
        var entityId = UUID.randomUUID().toString();
        var entity = TestEntity.Builder.newInstance().id(entityId).build();
        var retryProcess = new FutureRetryProcess<TestEntity, Object, String>("process", (e, i) -> completedFuture("content"))
                .entityReload(id -> StoreResult.alreadyLeased("error"));

        var future = retryProcess.execute(new ProcessContext<>(entity, "any"));

        assertThat(future).failsWithin(timeout).withThrowableOfType(ExecutionException.class)
                .extracting(Throwable::getCause).isInstanceOfSatisfying(UnrecoverableEntityStateException.class, exception -> {
                    assertThat(exception.getEntity()).isSameAs(entity);
                    assertThat(exception.getProcessName()).isEqualTo("process");
                    assertThat(exception.getMessage()).isEqualTo("Cannot reload entity: error");
                });
    }
}
