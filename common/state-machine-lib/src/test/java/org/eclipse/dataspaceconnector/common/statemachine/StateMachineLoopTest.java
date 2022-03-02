package org.eclipse.dataspaceconnector.common.statemachine;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.retry.WaitStrategy;
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

class StateMachineLoopTest {

    private final WaitStrategy waitStrategy = mock(WaitStrategy.class);
    private final Monitor monitor = mock(Monitor.class);

    @BeforeEach
    void setUp() {
        when(waitStrategy.waitForMillis()).thenReturn(1L);
    }

    @Test
    void shouldExecuteProcessorsAsyncAndCanBeStopped() throws InterruptedException {
        var latch = new CountDownLatch(2);
        var processor = mock(EntitiesProcessor.class);
        when(processor.run()).thenAnswer(i -> {
            latch.countDown();
            Thread.sleep(100L);
            return 1L;
        });
        var loop = StateMachineLoop.Builder.newInstance("test", monitor, waitStrategy)
                .processor(processor)
                .shutdownTimeout(1)
                .build();

        loop.start();
        assertThat(latch.await(1, SECONDS)).isTrue();
        verify(processor, atLeastOnce()).run();

        assertThat(loop.stop()).succeedsWithin(2, SECONDS);
        verifyNoMoreInteractions(processor);
    }

    @Test
    void shouldNotWaitForSomeTimeIfTheresAtLeastOneProcessedEntity() throws InterruptedException {
        var processor = mock(EntitiesProcessor.class);
        when(processor.run()).thenReturn(1L);
        var latch = new CountDownLatch(1);
        doAnswer(i -> {
            latch.countDown();
            return 1L;
        }).when(waitStrategy).success();
        var loop = StateMachineLoop.Builder.newInstance("test", monitor, waitStrategy)
                .processor(processor)
                .build();

        loop.start();

        assertThat(latch.await(1, SECONDS)).isTrue();
        verify(waitStrategy, never()).waitForMillis();
        verify(waitStrategy, atLeastOnce()).success();
    }

    @Test
    void shouldWaitForSomeTimeIfNoEntityIsProcessed() throws InterruptedException {
        var processor = mock(EntitiesProcessor.class);
        when(processor.run()).thenReturn(0L);
        var latch = new CountDownLatch(1);
        var waitStrategy = mock(WaitStrategy.class);
        doAnswer(i -> {
            latch.countDown();
            return 0L;
        }).when(waitStrategy).success();
        var loop = StateMachineLoop.Builder.newInstance("test", monitor, waitStrategy)
                .processor(processor)
                .build();

        loop.start();

        assertThat(latch.await(1, SECONDS)).isTrue();
        verify(waitStrategy, atLeastOnce()).waitForMillis();
        verify(waitStrategy, atLeastOnce()).success();
    }

    @Test
    void shouldExitWithAnExceptionIfProcessorExitsWithAnUnrecoverableError() {
        var processor = mock(EntitiesProcessor.class);
        when(processor.run()).thenThrow(new Error("unrecoverable"));
        var loop = StateMachineLoop.Builder.newInstance("test", monitor, waitStrategy)
                .processor(processor)
                .build();

        assertThat(loop.start()).succeedsWithin(1, SECONDS);
        assertThat(loop.isActive()).isFalse();
    }

    @Test
    void shouldWaitRetryTimeWhenAnExceptionIsThrownByAnProcessor() throws InterruptedException {
        var latch = new CountDownLatch(1);
        var processor = mock(EntitiesProcessor.class);
        when(processor.run()).thenThrow(new EdcException("exception")).thenReturn(0L);
        when(waitStrategy.retryInMillis()).thenAnswer(i -> {
            latch.countDown();
            return 1L;
        });
        var loop = StateMachineLoop.Builder.newInstance("test", monitor, waitStrategy)
                .processor(processor)
                .build();

        loop.start();

        assertThat(latch.await(1, SECONDS)).isTrue();
        assertThat(loop.isActive()).isTrue();
        verify(waitStrategy).retryInMillis();
    }
}