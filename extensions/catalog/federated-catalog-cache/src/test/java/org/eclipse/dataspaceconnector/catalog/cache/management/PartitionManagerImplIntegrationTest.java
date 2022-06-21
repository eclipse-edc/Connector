/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.cache.management;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.cache.DefaultWorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.cache.crawler.CrawlerImpl;
import org.eclipse.dataspaceconnector.catalog.cache.crawler.NodeQueryAdapterRegistryImpl;
import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.createWorkItem;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This tests the PartitionManagerImpl with real crawlers and in a real multithreading environment.
 * It uses several dummy classes which are private static classes.
 */
class PartitionManagerImplIntegrationTest {
    public static final int WORK_ITEM_COUNT = 1000;
    private final Monitor monitorMock = mock(Monitor.class);
    private WorkItemQueue signallingWorkItemQueue;
    private List<WorkItem> staticWorkLoad;
    private Function<WorkItemQueue, Crawler> generatorFunction;
    private CountDownLatch latch;
    private WorkQueueListener queueListener;

    @BeforeEach
    void setup() {

        latch = new CountDownLatch(WORK_ITEM_COUNT);
        queueListener = mock(WorkQueueListener.class);
        signallingWorkItemQueue = new SignalingWorkItemQueue(WORK_ITEM_COUNT + 1, queueListener);
        staticWorkLoad = IntStream.range(0, WORK_ITEM_COUNT).mapToObj(i -> createWorkItem()).collect(Collectors.toList());

        NodeQueryAdapter adapterMock = mock(NodeQueryAdapter.class);
        when(adapterMock.sendRequest(isA(UpdateRequest.class))).thenReturn(CompletableFuture.completedFuture(new UpdateResponse()));

        var registry = new NodeQueryAdapterRegistryImpl();
        registry.register("test-protocol", adapterMock);

        BlockingQueue<UpdateResponse> loaderQueueMock = mock(BlockingQueue.class);
        generatorFunction = workItemQueue -> CrawlerImpl.Builder.newInstance()
                .retryPolicy(new RetryPolicy<>())
                .monitor(monitorMock)
                .workQueuePollTimeout(() -> Duration.of(1, ChronoUnit.MILLIS)) //basically don't wait during polling the queue
                .workItems(workItemQueue)
                .protocolAdapters(registry)
                .errorReceiver(workItem -> {
                })
                .queue(loaderQueueMock)
                .build();

    }

    @ParameterizedTest
    @ValueSource(ints = { 10, 50, 500 })
    @DisplayName("Verify that " + WORK_ITEM_COUNT + " work items are correctly processed by a number of crawlers")
    void runManyCrawlers_verifyCompletion(int crawlerCount) throws InterruptedException {
        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(queueListener).unlocked();
        var partitionManager = new PartitionManagerImpl(monitorMock, signallingWorkItemQueue, generatorFunction, crawlerCount, () -> staticWorkLoad);

        partitionManager.schedule(new RunOnceExecutionPlan());

        assertThat(latch.await(1, TimeUnit.MINUTES)).withFailMessage("latch was expected to be 0 but was: " + latch.getCount()).isTrue();
        verify(queueListener, atLeastOnce()).unlocked();
    }

    /**
     * listens for events on the {@link SignalingWorkItemQueue}
     */
    private interface WorkQueueListener {
        default void locked() {
        }

        default void unlocked() {
        }

        default void tryLock() {
        }

        default void polled() {
        }
    }

    /**
     * A test work item queue that informs a registered listener whenever an
     * event like poll() or unlock() occurs.
     * The recommended pattern is to supply {@code mock(WorkQueueListener.class)}
     */
    private static class SignalingWorkItemQueue extends DefaultWorkItemQueue {
        private final WorkQueueListener listener;

        SignalingWorkItemQueue(int cap, WorkQueueListener listener) {
            super(cap);
            this.listener = listener;
        }

        @Override
        public void lock() {
            listener.locked();
            super.lock();
        }

        @Override
        public void unlock() {
            listener.unlocked();
            super.unlock();
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit) {
            listener.tryLock();
            return super.tryLock(timeout, unit);
        }

        @Override
        public WorkItem poll(long timeout, TimeUnit unit) throws InterruptedException {
            var polledItem = super.poll(timeout, unit);
            listener.polled();
            return polledItem;
        }
    }

    /**
     * An ExecutionPlan that runs a given {@link Runnable} right away and without recurrence
     */
    private static class RunOnceExecutionPlan implements ExecutionPlan {

        RunOnceExecutionPlan() {
        }

        @Override
        public ExecutionPlan merge(ExecutionPlan other) {
            return other;
        }

        @Override
        public void run(Runnable task) {
            Executors.newSingleThreadScheduledExecutor().submit(task);
        }
    }
}