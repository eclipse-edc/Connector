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

import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.statemachine.retry.TestEntity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

class ResultRetryProcessTest {

    private final Duration timeout = Duration.of(1, SECONDS);

    @Test
    void shouldComplete_whenResultSucceeds() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateCount(1).build();
        Process<TestEntity, Object, Object> retryProcess = Process.result("description", (e, i) -> StatusResult.success("output"));

        var future = retryProcess.execute(new ProcessContext<>(entity, "input"));

        assertThat(future).succeedsWithin(timeout);
    }

    @Test
    void shouldFail_whenResultFails() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateCount(1).build();
        Process<TestEntity, Object, Object> retryProcess = Process.result("description", (e, i) -> StatusResult.failure(ERROR_RETRY, "error"));

        var future = retryProcess.execute(new ProcessContext<>(entity, "input"));

        assertThat(future).failsWithin(timeout).withThrowableOfType(ExecutionException.class)
                .extracting(Throwable::getCause).isInstanceOfSatisfying(EntityStateException.class, exception -> {
                    assertThat(exception.getEntity()).isSameAs(entity);
                    assertThat(exception.getProcessName()).isEqualTo("description");
                    assertThat(exception.getMessage()).isEqualTo("error");
                });
    }

    @Test
    void shouldFailUnrecoverable_whenResultFatalError() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateCount(1).build();
        Process<TestEntity, Object, Object> retryProcess = Process.result("description", (e, i) -> StatusResult.failure(FATAL_ERROR, "error"));

        var future = retryProcess.execute(new ProcessContext<>(entity, "input"));

        assertThat(future).failsWithin(timeout).withThrowableOfType(ExecutionException.class)
                .extracting(Throwable::getCause).isInstanceOfSatisfying(UnrecoverableEntityStateException.class, exception -> {
                    assertThat(exception.getEntity()).isSameAs(entity);
                    assertThat(exception.getProcessName()).isEqualTo("description");
                    assertThat(exception.getMessage()).isEqualTo("error");
                });
    }

    @Test
    void shouldFailUnrecoverable_whenFunctionThrowsException() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateCount(1).build();
        Process<TestEntity, Object, Object> retryProcess = Process.result("description", (testEntity, o) -> {
            throw new RuntimeException("unexpected exception");
        });

        var future = retryProcess.execute(new ProcessContext<>(entity, "input"));

        assertThat(future).failsWithin(timeout).withThrowableOfType(ExecutionException.class)
                .extracting(Throwable::getCause).isInstanceOfSatisfying(RuntimeException.class, exception -> {
                    assertThat(exception.getMessage()).isEqualTo("unexpected exception");
                });
    }

}
