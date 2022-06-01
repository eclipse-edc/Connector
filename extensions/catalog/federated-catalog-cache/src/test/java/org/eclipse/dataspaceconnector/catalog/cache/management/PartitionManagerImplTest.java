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

import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.createWorkItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PartitionManagerImplTest {

    private final Monitor monitorMock = mock(Monitor.class);
    private final WorkItemQueue workItemQueueMock = mock(WorkItemQueue.class);

    private PartitionManagerImpl partitionManager;
    private List<WorkItem> staticWorkload;

    @BeforeEach
    void setup() {
        staticWorkload = List.of(createWorkItem());
        Function<WorkItemQueue, Crawler> crawlerGenerator = workItems -> mock(Crawler.class);
        partitionManager = new PartitionManagerImpl(monitorMock, workItemQueueMock, crawlerGenerator, 5, () -> staticWorkload);
    }

    @Test
    @DisplayName("expect the workload to be put into the work item queue")
    void schedule() {
        partitionManager.schedule(Runnable::run);

        verify(workItemQueueMock).lock();
        verify(workItemQueueMock).addAll(staticWorkload);
        verify(workItemQueueMock).unlock();
    }

    @Test
    void stop_allCrawlersJoinSuccessfully() throws InterruptedException {
        var latch = new CountDownLatch(5);
        partitionManager = new PartitionManagerImpl(monitorMock, workItemQueueMock, workItems -> {
            Crawler crawler = mock(Crawler.class);
            doAnswer(i -> {
                latch.countDown();
                return null;
            }).when(crawler).run();
            when(crawler.join()).thenReturn(true);
            return crawler;
        }, 5, () -> staticWorkload);

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        partitionManager.stop();
    }

    @Test
    void schedule_verifyNodeDirectoryGetsQueried() {
        Supplier<List<WorkItem>> queueSourceMock = mock(Supplier.class);
        when(queueSourceMock.get()).thenReturn(List.of(new WorkItem("http://some.url", "test-protocol")));
        partitionManager = new PartitionManagerImpl(monitorMock, workItemQueueMock, workItems -> mock(Crawler.class), 5, queueSourceMock);

        ExecutionPlan runMultiPlan = mock(ExecutionPlan.class);
        doAnswer(invocation -> {
            var runnable = (Runnable) invocation.getArgument(0);
            runnable.run(); //run several times
            runnable.run();
            runnable.run();
            return null;
        }).when(runMultiPlan).run(any());

        // schedule once, make sure multiple invocations happen
        partitionManager.schedule(runMultiPlan);

        verify(queueSourceMock, times(3)).get();
    }

    @Test
    void schedule_planThrowsIllegalStateException_shouldLogException() throws InterruptedException {
        var plan = mock(ExecutionPlan.class);
        when(workItemQueueMock.addAll(any()))
                .thenReturn(true) // first time works
                .thenThrow(new IllegalStateException("Queue full")); //second time fails

        var latch = new CountDownLatch(2);
        doAnswer(invocation -> {
            var runnable = (Runnable) invocation.getArgument(0);
            runnable.run();
            latch.countDown();
            Thread.sleep(100);
            runnable.run();
            latch.countDown();
            return null;
        }).when(plan).run(any());
        partitionManager.schedule(plan);

        // assert exception was thrown and logged
        verify(workItemQueueMock, times(2)).addAll(any());
        assertThat(latch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
        verify(workItemQueueMock, times(2)).lock();
        verify(workItemQueueMock, times(2)).unlock();
        verify(monitorMock).warning(startsWith("Cannot add 1 elements to the queue"), isA(IllegalStateException.class));
    }

    @Test
    void schedule_planThrowsAnyException_shouldLogException() throws InterruptedException {
        var plan = mock(ExecutionPlan.class);
        when(workItemQueueMock.addAll(any()))
                .thenReturn(true) // first time works
                .thenThrow(new RuntimeException("Any random error")); //second time fails

        var latch = new CountDownLatch(2);
        doAnswer(invocation -> {
            var runnable = (Runnable) invocation.getArgument(0);
            runnable.run();
            latch.countDown();
            Thread.sleep(100);
            runnable.run();
            latch.countDown();
            return null;
        }).when(plan).run(any());
        partitionManager.schedule(plan);

        // assert exception was thrown and logged
        verify(workItemQueueMock, times(2)).addAll(any());
        assertThat(latch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
        verify(workItemQueueMock, times(2)).lock();
        verify(workItemQueueMock, times(2)).unlock();
        verify(monitorMock).severe(startsWith("Error populating the queue"), isA(RuntimeException.class));
    }
}
