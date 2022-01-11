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
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.dataplane.framework.manager;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneManagerImplTest {
    private PipelineService pipelineService;
    private DataPlaneManagerImpl dataPlaneManager;

    /**
     * Verifies a request is enqueued, dequeued, and dispatched to the pipeline service.
     */
    @Test
    void verifyWorkDispatch() throws InterruptedException {

        var latch = new CountDownLatch(1);

        when(pipelineService.transfer(isA(DataFlowRequest.class))).thenAnswer(i -> {
            latch.countDown();
            return completedFuture(Result.success("ok"));
        });

        DataFlowRequest request = createRequest();

        dataPlaneManager.start();

        dataPlaneManager.initiateTransfer(request);

        latch.await(10000, TimeUnit.MILLISECONDS);

        dataPlaneManager.stop();

        verify(pipelineService, times(1)).transfer(isA(DataFlowRequest.class));
    }

    /**
     * Verifies that the dispatch thread survives an error thrown by a worker.
     */
    @Test
    void verifyWorkDispatchError() throws InterruptedException {
        var latch = new CountDownLatch(1);

        when(pipelineService.transfer(isA(DataFlowRequest.class)))
                .thenAnswer(i -> {
                    throw new RuntimeException("Test exception");
                }).thenAnswer((i -> {
                    latch.countDown();
                    return completedFuture(Result.success("ok"));
                }));

        DataFlowRequest request = createRequest();

        dataPlaneManager.start();

        dataPlaneManager.initiateTransfer(request);
        dataPlaneManager.initiateTransfer(request);

        latch.await(10000, TimeUnit.MILLISECONDS);

        dataPlaneManager.stop();

        verify(pipelineService, times(2)).transfer(isA(DataFlowRequest.class));
    }

    @BeforeEach
    void setUp() {
        pipelineService = mock(PipelineService.class);
        var monitor = mock(Monitor.class);

        dataPlaneManager = DataPlaneManagerImpl.Builder.newInstance()
                .queueCapacity(100)
                .workers(1)
                .waitTimeout(10)
                .pipelineService(pipelineService)
                .monitor(monitor).build();
    }

    private DataFlowRequest createRequest() {
        return DataFlowRequest.Builder.newInstance()
                .id("1")
                .processId("1")
                .transferType(TransferType.Builder.transferType().contentType("application/octet-stream").build())
                .sourceDataAddress(DataAddress.Builder.newInstance().build())
                .destinationDataAddress(DataAddress.Builder.newInstance().build())
                .build();
    }

}
