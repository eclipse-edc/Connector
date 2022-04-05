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
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.createWorkItem;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PartitionManagerImplTest {

    private PartitionManagerImpl partitionManager;

    private final Monitor monitorMock = mock(Monitor.class);
    private final WorkItemQueue workItemQueueMock = mock(WorkItemQueue.class);
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

}
