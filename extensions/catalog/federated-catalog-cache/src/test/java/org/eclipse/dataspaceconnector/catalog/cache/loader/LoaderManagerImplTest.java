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

package org.eclipse.dataspaceconnector.catalog.cache.loader;

import org.eclipse.dataspaceconnector.catalog.spi.Loader;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.retry.WaitStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoaderManagerImplTest {

    private final Loader loaderMock = mock(Loader.class);
    private final WaitStrategy waitStrategyMock = mock(WaitStrategy.class);
    private final int batchSize = 3;
    private final BlockingQueue<UpdateResponse> queue = new ArrayBlockingQueue<>(batchSize);
    private LoaderManagerImpl loaderManager;

    @BeforeEach
    void setup() {
        loaderManager = new LoaderManagerImpl(List.of(loaderMock), batchSize, waitStrategyMock, mock(Monitor.class));
    }

    @Test
    @DisplayName("Verify that the loader manager waits one pass when the queue does not yet contain sufficient elements")
    void batchSizeNotReachedWithinTimeframe() throws InterruptedException {
        range(0, batchSize - 1).forEach(i -> queue.offer(new UpdateResponse()));
        var completionSignal = new CountDownLatch(1);
        when(waitStrategyMock.retryInMillis()).thenAnswer(i -> {
            completionSignal.countDown();
            return 2L;
        });

        loaderManager.start(queue);

        assertThat(completionSignal.await(300L, TimeUnit.MILLISECONDS)).isTrue();
        verify(waitStrategyMock, atLeastOnce()).retryInMillis();
    }

    @Test
    @DisplayName("Verify that the LoaderManager does not sleep when a complete batch was processed")
    void batchSizeReachedWithinTimeframe() throws InterruptedException {
        range(0, batchSize).forEach(i -> queue.offer(new UpdateResponse()));
        var completionSignal = new CountDownLatch(1);

        doAnswer(i -> {
            completionSignal.countDown();
            return null;
        }).when(waitStrategyMock).success();

        loaderManager.start(queue);

        assertThat(completionSignal.await(5, TimeUnit.SECONDS)).isTrue();

        verify(loaderMock, times(1)).load(any());
        verify(waitStrategyMock).success();
    }

}
