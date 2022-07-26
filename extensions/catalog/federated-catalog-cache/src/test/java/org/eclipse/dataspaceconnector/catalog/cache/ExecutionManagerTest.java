/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.catalog.spi.CrawlerSuccessHandler;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.delayedExecutor;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.TEST_PROTOCOL;
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.createCatalog;
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.createNode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ExecutionManagerTest {

    private final FederatedCacheNodeDirectory nodeDirectoryMock = mock(FederatedCacheNodeDirectory.class);
    private final Monitor monitorMock = mock(Monitor.class);
    private final NodeQueryAdapterRegistry nodeQueryAdapterRegistry = mock(NodeQueryAdapterRegistry.class);
    private final Runnable preExecutionTaskMock = mock(Runnable.class);
    private final NodeQueryAdapter queryAdapterMock = mock(NodeQueryAdapter.class);
    private final CrawlerSuccessHandler successConsumerMock = mock(CrawlerSuccessHandler.class);
    private final Runnable postExecutionTask = mock(Runnable.class);
    private ExecutionManager manager;

    @BeforeEach
    void setUp() {
        manager = createManager();
    }

    @Test
    void executePlan() {
        when(nodeDirectoryMock.getAll()).thenReturn(List.of(createNode()));
        when(nodeQueryAdapterRegistry.findForProtocol(TEST_PROTOCOL)).thenReturn(List.of(queryAdapterMock));
        when(queryAdapterMock.sendRequest(any())).thenReturn(completedFuture(new UpdateResponse("test-url", createCatalog())));
        manager.executePlan(simplePlan());

        var inOrder = inOrder(preExecutionTaskMock, queryAdapterMock, successConsumerMock);
        inOrder.verify(preExecutionTaskMock).run();
        inOrder.verify(queryAdapterMock).sendRequest(any());
        inOrder.verify(successConsumerMock).accept(any());
    }

    @Test
    void executePlan_waitsForCrawler() {
        when(nodeDirectoryMock.getAll()).thenReturn(List.of(createNode(), createNode()));
        when(nodeQueryAdapterRegistry.findForProtocol(TEST_PROTOCOL)).thenReturn(List.of(queryAdapterMock));

        var response = new UpdateResponse("test-url", createCatalog());
        var future = new CompletableFuture<UpdateResponse>();
        future.completeAsync(() -> response, delayedExecutor(2, TimeUnit.SECONDS));

        when(queryAdapterMock.sendRequest(any())).thenReturn(future);
        manager.executePlan(simplePlan());

        var inOrder = inOrder(preExecutionTaskMock, queryAdapterMock, successConsumerMock);
        inOrder.verify(preExecutionTaskMock).run();
        inOrder.verify(queryAdapterMock).sendRequest(any());
        inOrder.verify(successConsumerMock).accept(any());
        verify(monitorMock, atLeastOnce()).debug(startsWith("ExecutionManager: No crawler available"));
    }


    @Test
    void executePlan_noQueryAdapter() {
        when(nodeDirectoryMock.getAll()).thenReturn(List.of(createNode()));
        when(nodeQueryAdapterRegistry.findForProtocol(TEST_PROTOCOL)).thenReturn(List.of());

        manager.executePlan(simplePlan());

        var inOrder = inOrder(preExecutionTaskMock);
        inOrder.verify(preExecutionTaskMock).run();
        verifyNoInteractions(queryAdapterMock, successConsumerMock);
    }

    @Test
    void executePlan_preTaskThrowsException() {
        doThrow(new RuntimeException("test-exception")).when(preExecutionTaskMock).run();
        when(nodeDirectoryMock.getAll()).thenReturn(List.of(createNode()));
        when(nodeQueryAdapterRegistry.findForProtocol(TEST_PROTOCOL)).thenReturn(List.of(queryAdapterMock));
        when(queryAdapterMock.sendRequest(any())).thenReturn(completedFuture(new UpdateResponse("test-url", createCatalog())));
        manager.executePlan(simplePlan());

        verify(successConsumerMock).accept(any());
        verify(monitorMock, atLeastOnce()).severe(anyString(), any(Throwable.class));
    }

    @Test
    void executePlan_postTaskThrowsException() {
        doThrow(new RuntimeException("test-exception")).when(postExecutionTask).run();
        when(nodeDirectoryMock.getAll()).thenReturn(List.of(createNode()));
        when(nodeQueryAdapterRegistry.findForProtocol(TEST_PROTOCOL)).thenReturn(List.of(queryAdapterMock));
        when(queryAdapterMock.sendRequest(any())).thenReturn(completedFuture(new UpdateResponse("test-url", createCatalog())));
        manager.executePlan(simplePlan());

        verify(successConsumerMock).accept(any());
        verify(monitorMock, atLeastOnce()).severe(anyString(), any(Throwable.class));
    }

    @Test
    void executePlan_completesExceptionally() {
        when(nodeDirectoryMock.getAll()).thenReturn(List.of(createNode()));
        when(nodeQueryAdapterRegistry.findForProtocol(TEST_PROTOCOL)).thenReturn(List.of(queryAdapterMock));
        var exc = new EdcException("some exception");
        when(queryAdapterMock.sendRequest(any())).thenReturn(failedFuture(exc));
        manager.executePlan(simplePlan());

        var inOrder = inOrder(preExecutionTaskMock, queryAdapterMock, successConsumerMock);
        inOrder.verify(preExecutionTaskMock).run();
        inOrder.verify(queryAdapterMock).sendRequest(any());

        verifyNoInteractions(successConsumerMock);
        verify(monitorMock, atLeastOnce()).severe(anyString(), isA(CompletionException.class));
    }

    @Test
    void executePlan_workItemsEmpty() {
        when(nodeDirectoryMock.getAll()).thenReturn(List.of());
        manager.executePlan(simplePlan());
        var inorder = inOrder(preExecutionTaskMock, monitorMock);

        inorder.verify(monitorMock).info(anyString());
        inorder.verify(preExecutionTaskMock).run();
        inorder.verify(monitorMock).info(anyString());
        inorder.verify(monitorMock).warning(startsWith("No WorkItems found"));
        inorder.verify(monitorMock).info(anyString());
        verifyNoMoreInteractions(monitorMock);
        verifyNoInteractions(nodeQueryAdapterRegistry);
    }


    private ExecutionPlan simplePlan() {
        return Runnable::run;
    }

    private ExecutionManager createManager() {
        return ExecutionManager.Builder.newInstance()
                .nodeDirectory(nodeDirectoryMock)
                .nodeQueryAdapterRegistry(nodeQueryAdapterRegistry)
                .connectorId("test-connector")
                .preExecutionTask(preExecutionTaskMock)
                .postExecutionTask(postExecutionTask)
                .monitor(monitorMock)
                .onSuccess(successConsumerMock)
                .build();
    }
}