package org.eclipse.dataspaceconnector.common.statemachine;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.retry.WaitStrategy;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class StateMachineTest {

    private final WaitStrategy waitStrategy = mock(WaitStrategy.class);
    private final Monitor monitor = mock(Monitor.class);
    private final ExecutorInstrumentation instrumentation = ExecutorInstrumentation.noop();

    @BeforeEach
    void setUp() {
        when(waitStrategy.waitForMillis()).thenReturn(1L);
    }

    @Test
    void shouldExecuteProcessorsAsyncAndCanBeStopped() throws InterruptedException {
        var latch = new CountDownLatch(2);
        var processor = mock(StateProcessor.class);
        when(processor.process()).thenAnswer(i -> {
            latch.countDown();
            Thread.sleep(100L);
            return 1L;
        });
        var stateMachine = StateMachine.Builder.newInstance("test", monitor, instrumentation, waitStrategy)
                .processor(processor)
                .shutdownTimeout(1)
                .build();

        stateMachine.start();
        assertThat(latch.await(1, SECONDS)).isTrue();
        verify(processor, atLeastOnce()).process();

        assertThat(stateMachine.stop()).succeedsWithin(2, SECONDS);
        verifyNoMoreInteractions(processor);
    }

    @Test
    void shouldNotWaitForSomeTimeIfTheresAtLeastOneProcessedEntity() throws InterruptedException {
        var processor = mock(StateProcessor.class);
        when(processor.process()).thenReturn(1L);
        var latch = new CountDownLatch(1);
        doAnswer(i -> {
            latch.countDown();
            return 1L;
        }).when(waitStrategy).success();
        var stateMachine = StateMachine.Builder.newInstance("test", monitor, instrumentation, waitStrategy)
                .processor(processor)
                .build();

        stateMachine.start();

        assertThat(latch.await(1, SECONDS)).isTrue();
        verify(waitStrategy, never()).waitForMillis();
        verify(waitStrategy, atLeastOnce()).success();
    }

    @Test
    void shouldWaitForSomeTimeIfNoEntityIsProcessed() throws InterruptedException {
        var processor = mock(StateProcessor.class);
        when(processor.process()).thenReturn(0L);
        var latch = new CountDownLatch(1);
        var waitStrategy = mock(WaitStrategy.class);
        doAnswer(i -> {
            latch.countDown();
            return 0L;
        }).when(waitStrategy).waitForMillis();
        var stateMachine = StateMachine.Builder.newInstance("test", monitor, instrumentation, waitStrategy)
                .processor(processor)
                .build();

        stateMachine.start();

        assertThat(latch.await(1, SECONDS)).isTrue();
        verify(waitStrategy, atLeastOnce()).waitForMillis();
        verify(waitStrategy, atLeastOnce()).success();
    }

    @Test
    void shouldExitWithAnExceptionIfProcessorExitsWithAnUnrecoverableError() {
        var processor = mock(StateProcessor.class);
        when(processor.process()).thenThrow(new Error("unrecoverable"));
        var stateMachine = StateMachine.Builder.newInstance("test", monitor, instrumentation, waitStrategy)
                .processor(processor)
                .build();

        assertThat(stateMachine.start()).succeedsWithin(1, SECONDS);
        assertThat(stateMachine.isActive()).isFalse();
    }

    @Test
    void shouldWaitRetryTimeWhenAnExceptionIsThrownByAnProcessor() throws InterruptedException {
        var latch = new CountDownLatch(1);
        var processor = mock(StateProcessor.class);
        when(processor.process()).thenThrow(new EdcException("exception")).thenReturn(0L);
        when(waitStrategy.retryInMillis()).thenAnswer(i -> {
            latch.countDown();
            return 1L;
        });
        var stateMachine = StateMachine.Builder.newInstance("test", monitor, instrumentation, waitStrategy)
                .processor(processor)
                .build();

        stateMachine.start();

        assertThat(latch.await(1, SECONDS)).isTrue();
        assertThat(stateMachine.isActive()).isTrue();
        verify(waitStrategy).retryInMillis();
    }
}