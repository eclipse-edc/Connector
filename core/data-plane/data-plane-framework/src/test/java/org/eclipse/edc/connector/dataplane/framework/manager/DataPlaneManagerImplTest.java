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

package org.eclipse.edc.connector.dataplane.framework.manager;

import org.eclipse.edc.connector.dataplane.framework.store.InMemoryDataPlaneStore;
import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class DataPlaneManagerImplTest {
    TransferService transferService = mock();
    DataPlaneStore store = new InMemoryDataPlaneStore(10);
    DataFlowRequest request = createRequest();
    TransferServiceRegistry registry = mock();

    @BeforeEach
    public void setUp() {
        when(registry.resolveTransferService(request))
                .thenReturn(transferService);
    }

    /**
     * Verifies a request is enqueued, dequeued, and dispatched to the pipeline service.
     */
    @Test
    void verifyWorkDispatch() {
        var dataPlaneManager = createDataPlaneManager();

        when(registry.resolveTransferService(request))
                .thenReturn(transferService);
        when(transferService.canHandle(isA(DataFlowRequest.class)))
                .thenReturn(true);


        when(transferService.transfer(isA(DataFlowRequest.class)))
                .thenAnswer(i -> completedFuture(Result.success("ok")));

        dataPlaneManager.start();
        dataPlaneManager.initiateTransfer(request);

        await().untilAsserted(() -> {
            verify(registry).resolveTransferService(eq(request));
            verify(transferService).transfer(isA(DataFlowRequest.class));
        });
    }

    /**
     * Verifies that the dispatch thread survives an error thrown by a worker.
     */
    @Test
    void verifyWorkDispatchError() {
        var dataPlaneManager = createDataPlaneManager();

        when(transferService.canHandle(request))
                .thenReturn(true);

        when(transferService.transfer(request))
                .thenAnswer(i -> {
                    throw new RuntimeException("Test exception");
                })
                .thenAnswer((i -> completedFuture(Result.success("ok"))));


        dataPlaneManager.start();

        dataPlaneManager.initiateTransfer(request);
        dataPlaneManager.initiateTransfer(request);

        await().untilAsserted(() -> {
            verify(transferService, times(2)).transfer(request);
        });
    }

    @Test
    void verifyWorkDispatch_onUnavailableTransferService_completesTransfer() {
        // Modify store used in createDataPlaneManager()
        store = mock(DataPlaneStore.class);

        var dataPlaneManager = createDataPlaneManager();

        doAnswer(i -> null).when(registry).resolveTransferService(request);
        doAnswer(i -> null).when(store).completed(request.getProcessId());

        dataPlaneManager.start();
        dataPlaneManager.initiateTransfer(request);

        await().untilAsserted(() -> {
            verify(store, times(1)).completed(request.getProcessId());
        });
    }

    DataFlowRequest createRequest() {
        return DataFlowRequest.Builder.newInstance()
                .id("1")
                .processId("1")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("type").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("type").build())
                .build();
    }

    private DataPlaneManagerImpl createDataPlaneManager() {
        return DataPlaneManagerImpl.Builder.newInstance()
                .queueCapacity(100)
                .workers(1)
                .executorInstrumentation(ExecutorInstrumentation.noop())
                .waitTimeout(10)
                .transferServiceRegistry(registry)
                .store(store)
                .transferProcessClient(mock())
                .monitor(mock())
                .build();
    }

}
