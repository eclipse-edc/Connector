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

package org.eclipse.dataspaceconnector.catalog.cache.crawler;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.cache.DefaultWorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.CrawlerErrorHandler;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.createWorkItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrawlerImplTest {
    public static final int QUEUE_CAPACITY = 3;
    public static final int JOIN_WAIT_TIME = 1000;
    public static final int WORK_QUEUE_POLL_TIMEOUT = 500;
    private CrawlerImpl crawler;
    private NodeQueryAdapter protocolAdapterMock;
    private ArrayBlockingQueue<UpdateResponse> queue;
    private Monitor monitorMock;
    private WorkItemQueue workQueue;
    private NodeQueryAdapterRegistry registry;
    private CrawlerErrorHandler errorHandlerMock;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newSingleThreadExecutor();
        errorHandlerMock = mock(CrawlerErrorHandler.class);
        protocolAdapterMock = mock(NodeQueryAdapter.class);
        queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        monitorMock = mock(Monitor.class);
        workQueue = new DefaultWorkItemQueue(10);
        registry = mock(NodeQueryAdapterRegistry.class);
        when(registry.findForProtocol(anyString())).thenReturn(Collections.singletonList(protocolAdapterMock));
        crawler = new CrawlerImpl(workQueue, monitorMock, queue, createRetryPolicy(), registry, () -> Duration.ofMillis(WORK_QUEUE_POLL_TIMEOUT), errorHandlerMock);
    }

    @AfterEach
    void teardown() {
        executorService.shutdown();
    }

    @Test
    @DisplayName("Should insert one item into queue when request succeeds")
    void shouldInsertInQueue_whenSucceeds() throws InterruptedException {
        var l = new CountDownLatch(1);
        when(protocolAdapterMock.sendRequest(isA(UpdateRequest.class)))
                .thenAnswer(i -> {
                    l.countDown();
                    return CompletableFuture.completedFuture(new UpdateResponse());
                });

        workQueue.put(createWorkItem());
        executorService.submit(crawler);
        assertThat(l.await(JOIN_WAIT_TIME, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(1);
        verify(protocolAdapterMock).sendRequest(isA(UpdateRequest.class));
    }

    @Test
    @DisplayName("Should not insert into queue when the request fails")
    void shouldNotInsertInQueue_whenRequestFails() throws InterruptedException {

        var l = new CountDownLatch(1);

        when(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).thenAnswer(i -> {
            l.countDown();
            return CompletableFuture.failedFuture(new EdcException("not reachable"));
        });
        workQueue.put(createWorkItem());

        executorService.submit(crawler);

        assertThat(l.await(JOIN_WAIT_TIME, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(crawler.join()).isTrue();
        assertThat(queue).isEmpty();
        verify(protocolAdapterMock).sendRequest(isA(UpdateRequest.class));
    }

    @Test
    @DisplayName("Should insert only those items into queue that have succeeded")
    void shouldInsertInQueue_onlySuccessfulProtocolRequests() throws InterruptedException {

        var l = new CountDownLatch(2);
        NodeQueryAdapter secondAdapter = mock(NodeQueryAdapter.class);
        when(registry.findForProtocol(anyString())).thenReturn(Arrays.asList(protocolAdapterMock, secondAdapter));

        when(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).thenAnswer(i -> {
            l.countDown();
            return CompletableFuture.completedFuture(new UpdateResponse());
        });

        when(secondAdapter.sendRequest(isA(UpdateRequest.class))).thenAnswer(i -> {
            l.countDown();
            return CompletableFuture.failedFuture(new RuntimeException());
        });
        workQueue.put(createWorkItem());

        executorService.submit(crawler);

        assertThat(l.await(JOIN_WAIT_TIME, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(1);
        verify(protocolAdapterMock).sendRequest(isA(UpdateRequest.class));
        verify(registry).findForProtocol(anyString());
        verify(secondAdapter).sendRequest(isA(UpdateRequest.class));
    }

    @Test
    @DisplayName("Should not insert when Queue is at capacity")
    void shouldLogError_whenQueueFull() throws InterruptedException {
        range(0, QUEUE_CAPACITY).forEach(i -> queue.add(new UpdateResponse())); //queue is full now

        var l = new CountDownLatch(1);
        when(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).thenAnswer(i -> {
            l.countDown();
            return CompletableFuture.completedFuture(new UpdateResponse());
        });

        workQueue.put(createWorkItem());

        executorService.submit(crawler);

        assertThat(l.await(JOIN_WAIT_TIME, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(3);
        verify(protocolAdapterMock).sendRequest(isA(UpdateRequest.class));
    }

    @Test
    void shouldPauseWhenNoWorkItem() throws InterruptedException {

        executorService.submit(crawler);

        // wait until the queue has likely been polled at least once
        Thread.sleep(WORK_QUEUE_POLL_TIMEOUT);

        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(0);
    }

    @Test
    void shouldErrorOut_whenNoProtocolAdapterFound() throws InterruptedException {

        crawler = new CrawlerImpl(workQueue, monitorMock, queue, createRetryPolicy(), new NodeQueryAdapterRegistryImpl(), () -> Duration.ofMillis(500), errorHandlerMock);

        workQueue.put(createWorkItem());
        var l = new CountDownLatch(1);

        doAnswer(i -> {
            l.countDown();
            return null;
        }).when(errorHandlerMock).accept(isA(WorkItem.class));


        executorService.submit(crawler);

        assertThat(l.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(workQueue).hasSize(0); //1).allSatisfy(wi -> assertThat(wi.getErrors()).isNotNull().hasSize(1));
        verify(errorHandlerMock).accept(isA(WorkItem.class));
    }

    private RetryPolicy<Object> createRetryPolicy() {
        return new RetryPolicy<>().withMaxRetries(1);
    }

}