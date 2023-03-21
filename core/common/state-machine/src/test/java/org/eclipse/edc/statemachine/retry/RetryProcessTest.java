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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.statemachine.retry;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RetryProcessTest {

    private static final long DELAY = 2L;
    private final Monitor monitor = mock(Monitor.class);
    private final long millis = 123;
    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(millis), UTC);
    private final Supplier<Boolean> process = mock(Supplier.class);
    private final WaitStrategy fixedWaitStrategy = () -> DELAY;
    private final int retryLimit = 2;
    private final EntityRetryProcessConfiguration configuration = new EntityRetryProcessConfiguration(retryLimit, () -> fixedWaitStrategy);
    private final long shouldDelayTime = millis - DELAY + 1;
    private final long shouldNotDelayTime = millis - DELAY;

    @Test
    void execute_shouldNotProcess_whenItShouldDelay() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateTimestamp(shouldDelayTime).stateCount(2).build();
        var retryProcess = new TestRetryProcess(entity, configuration, monitor, clock);

        boolean any = retryProcess.execute("any");

        assertThat(any).isFalse();
        verifyNoInteractions(process);
    }

    @Test
    void execute_shouldNotProcess_whenItShouldDelayAndExecuteOnDelayIfSet() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateTimestamp(shouldDelayTime).stateCount(2).build();
        var onDelay = mock(Consumer.class);
        var retryProcess = new TestRetryProcess(entity, configuration, monitor, clock).onDelay(onDelay);

        boolean any = retryProcess.execute("any");

        assertThat(any).isFalse();
        verify(onDelay).accept(entity);
        verifyNoInteractions(process);
    }

    @Test
    void execute_shouldProcess_whenItIsNotRetry() {
        when(process.get()).thenReturn(true);
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateTimestamp(shouldDelayTime).stateCount(1).build();
        var onDelay = mock(Consumer.class);
        var retryProcess = new TestRetryProcess(entity, configuration, monitor, clock).onDelay(onDelay);

        boolean any = retryProcess.execute("any");

        assertThat(any).isTrue();
        verify(process).get();
    }

    @Test
    void execute_shouldProcess_whenItIsRetryButDoesNotDelay() {
        when(process.get()).thenReturn(true);
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateTimestamp(shouldNotDelayTime).stateCount(2).build();
        var retryProcess = new TestRetryProcess(entity, configuration, monitor, clock);

        boolean any = retryProcess.execute("any");

        assertThat(any).isTrue();
        verify(process).get();
    }

    @Test
    void retriesExhausted_shouldReturnTrueIfRetriesHaveBeenExhausted() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateCount(retryLimit + 1).build();
        var retryProcess = new TestRetryProcess(entity, configuration, monitor, clock);

        assertThat(retryProcess.retriesExhausted(entity)).isTrue();
    }

    @Test
    void retriesExhausted_shouldReturnFalseIfRetriesHaveNotBeenExhausted() {
        var entity = TestEntity.Builder.newInstance().id(UUID.randomUUID().toString()).stateCount(retryLimit).build();
        var retryProcess = new TestRetryProcess(entity, configuration, monitor, clock);

        assertThat(retryProcess.retriesExhausted(entity)).isFalse();
    }

    private class TestRetryProcess extends RetryProcess<TestEntity, TestRetryProcess> {

        protected TestRetryProcess(TestEntity entity, EntityRetryProcessConfiguration configuration, Monitor monitor, Clock clock) {
            super(entity, configuration, monitor, clock);
        }

        @Override
        boolean process(TestEntity entity, String description) {
            return process.get();
        }
    }

}
