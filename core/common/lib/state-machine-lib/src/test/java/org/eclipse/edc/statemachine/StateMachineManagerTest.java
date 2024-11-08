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

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


class StateMachineManagerTest {

    private final WaitStrategy waitStrategy = mock();
    private final Monitor monitor = mock();
    private final ExecutorInstrumentation instrumentation = ExecutorInstrumentation.noop();

    @BeforeEach
    void setUp() {
        when(waitStrategy.waitForMillis()).thenReturn(1L);
    }

    @Test
    void shouldExecuteProcessorsAsyncAndCanBeStopped() {
        var processor = mock(Processor.class);
        when(processor.process()).thenAnswer(i -> process(1));
        var stateMachine = StateMachineManager.Builder.newInstance("test", monitor, instrumentation, waitStrategy)
                .processor(processor)
                .shutdownTimeout(1)
                .build();

        stateMachine.start();

        await().untilAsserted(() -> {
            verify(processor, atLeastOnce()).process();

            assertThat(stateMachine.stop()).succeedsWithin(2, SECONDS);
            verifyNoMoreInteractions(processor);
        });
    }

    @Test
    void shouldNotWaitForSomeTimeIfTheresAtLeastOneProcessedEntity() {
        var processor = mock(Processor.class);
        when(processor.process()).thenReturn(1L);
        doAnswer(i -> 1L).when(waitStrategy).success();
        var stateMachine = StateMachineManager.Builder.newInstance("test", monitor, instrumentation, waitStrategy)
                .processor(processor)
                .build();

        stateMachine.start();

        await().untilAsserted(() -> {
            verify(waitStrategy, never()).waitForMillis();
            verify(waitStrategy, atLeastOnce()).success();
        });
    }

    @Test
    void shouldWaitForSomeTimeIfNoEntityIsProcessed() {
        var processor = mock(Processor.class);
        when(processor.process()).thenReturn(0L);
        var waitStrategy = mock(WaitStrategy.class);
        doAnswer(i -> 0L).when(waitStrategy).waitForMillis();
        var stateMachine = StateMachineManager.Builder.newInstance("test", monitor, instrumentation, waitStrategy)
                .processor(processor)
                .build();

        stateMachine.start();

        await().untilAsserted(() -> {
            verify(waitStrategy, atLeastOnce()).waitForMillis();
            verify(waitStrategy, atLeastOnce()).success();
        });
    }

    @Test
    void shouldExitWithAnExceptionIfProcessorExitsWithAnUnrecoverableError() {
        var processor = mock(Processor.class);
        when(processor.process()).thenThrow(new Error("unrecoverable"));
        var stateMachine = StateMachineManager.Builder.newInstance("test", monitor, instrumentation, waitStrategy)
                .processor(processor)
                .build();

        assertThat(stateMachine.start()).succeedsWithin(1, SECONDS);
        assertThat(stateMachine.isActive()).isFalse();
    }

    @Test
    void shouldWaitRetryTimeWhenAnExceptionIsThrownByAnProcessor() {
        var processor = mock(Processor.class);
        when(processor.process()).thenThrow(new EdcException("exception")).thenReturn(0L);
        when(waitStrategy.retryInMillis()).thenAnswer(i -> 1L);
        var stateMachine = StateMachineManager.Builder.newInstance("test", monitor, instrumentation, waitStrategy)
                .processor(processor)
                .build();

        stateMachine.start();

        await().untilAsserted(() -> {
            assertThat(stateMachine.isActive()).isTrue();
            verify(waitStrategy).retryInMillis();
        });
    }

    @Test
    void shouldExecuteStartupProcessorUntilItHasEntitiesToProcess() {
        var processor = mock(Processor.class);
        when(processor.process()).thenAnswer(i -> process(1));
        var startupProcessor = mock(Processor.class);
        when(startupProcessor.process()).thenAnswer(i -> process(1)).thenAnswer(i -> process(1)).thenAnswer(i -> process(0));
        var stateMachine = StateMachineManager.Builder.newInstance("test", monitor, instrumentation, waitStrategy)
                .startupProcessor(startupProcessor)
                .processor(processor)
                .shutdownTimeout(1)
                .build();

        stateMachine.start();

        await().untilAsserted(() -> {
            verify(processor, atLeast(2)).process();
            verify(startupProcessor, times(3)).process();

            assertThat(stateMachine.stop()).succeedsWithin(2, SECONDS);
            verifyNoMoreInteractions(processor);
        });
    }

    private long process(long count) {
        try {
            Thread.sleep(10L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return count;
    }
}
