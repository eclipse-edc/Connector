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

package org.eclipse.edc.catalog.cache;

import org.eclipse.edc.catalog.crawler.CatalogCrawlerManager;
import org.eclipse.edc.catalog.spi.CatalogCrawlerConfiguration;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.catalog.spi.model.CatalogUpdateResponse;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.crawler.spi.CrawlerAction;
import org.eclipse.edc.crawler.spi.CrawlerActionRegistry;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.crawler.spi.TargetNodeFilter;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CatalogCrawlerManagerTest {

    public static final String TEST_PROTOCOL = "test-protocol";
    private final TargetNodeDirectory nodeDirectoryMock = mock();
    private final Monitor monitorMock = mock();
    private final CrawlerActionRegistry crawlerActionRegistry = mock();
    private final Runnable preExecutionTaskMock = mock();
    private final CrawlerAction queryAdapterMock = mock();
    private final Runnable postExecutionTask = mock();
    private final FederatedCatalogCache store = mock();

    private CatalogCrawlerManager manager = createManagerBuilder().build();

    @Test
    void shouldStoreTheCatalog() {
        when(nodeDirectoryMock.getAll()).thenReturn(List.of(createNode()));
        when(crawlerActionRegistry.findForProtocol(any())).thenReturn(List.of(queryAdapterMock));
        var catalog = new Catalog();
        when(queryAdapterMock.apply(any())).thenReturn(completedFuture(new CatalogUpdateResponse("source", catalog)));

        manager.start();

        await().untilAsserted(() -> {
            verify(store).expireAll();
            verify(store).deleteExpired();

            verify(store).save(catalog);
        });
    }

    @Test
    void shouldNotQueryCatalog_whenProtocolVersionNotSupported() {
        when(nodeDirectoryMock.getAll()).thenReturn(List.of(createNode()));
        when(crawlerActionRegistry.findForProtocol(any())).thenReturn(List.of());

        manager.start();

        verifyNoInteractions(queryAdapterMock);
    }

    @Test
    void shouldLogSevere_whenUnexpectedException() {
        when(nodeDirectoryMock.getAll()).thenReturn(List.of(createNode()));
        when(crawlerActionRegistry.findForProtocol(TEST_PROTOCOL)).thenReturn(List.of(queryAdapterMock));
        var exc = new EdcException("unexpected exception");
        when(queryAdapterMock.apply(any())).thenReturn(failedFuture(exc));

        manager.start();

        await().untilAsserted(() -> {
            verify(monitorMock, atLeastOnce()).severe(startsWith("Unexpected exception occurred while crawling"), isA(CompletionException.class));
        });
    }

    @Test
    void shouldLogInfo_whenQueryReturnsConnectException() {
        when(nodeDirectoryMock.getAll()).thenReturn(List.of(createNode()));
        when(crawlerActionRegistry.findForProtocol(TEST_PROTOCOL)).thenReturn(List.of(queryAdapterMock));
        when(queryAdapterMock.apply(any())).thenReturn(failedFuture(new ConnectException("cannot connect")));

        manager.start();

        await().untilAsserted(() -> {
            verify(monitorMock, atLeastOnce()).info(startsWith("Cannot connect to node"));
        });
    }

    @Test
    void executePlan_withCustomFiltering() {
        when(nodeDirectoryMock.getAll()).thenReturn(List.of(createNode(), createNode(), createNode()));
        var filter = mock(TargetNodeFilter.class);
        manager = createManagerBuilder().nodeFilterFunction(filter).build();

        manager.start();

        await().untilAsserted(() -> {
            verify(filter, times(3)).test(any());
        });
    }

    @Test
    void executePlan_shouldNotRunPlanWhenGloballyDisabled() {
        manager = createManagerBuilder().configuration(disabledCrawler()).build();

        manager.start();

        verify(monitorMock).warning(anyString());
        verifyNoInteractions(preExecutionTaskMock, queryAdapterMock, postExecutionTask);
    }

    @Test
    void stop_shouldNotStopPlanWhenGloballyDisabled() {

        manager = createManagerBuilder().configuration(disabledCrawler()).build();
        manager.stop();

        verify(monitorMock).warning(anyString());
    }

    @Test
    void stop_shouldStopPlanWhenGloballyEnabled() {
        manager = createManagerBuilder().configuration(enabledCrawler()).build();
        manager.stop();

        verifyNoMoreInteractions(monitorMock);
    }

    private CatalogCrawlerConfiguration disabledCrawler() {
        return new CatalogCrawlerConfiguration(false, 5, 5, 5, 5, 5);
    }

    private CatalogCrawlerConfiguration enabledCrawler() {
        return new CatalogCrawlerConfiguration(true, 5, 20, 0, 5, 5);
    }

    @NotNull
    private CatalogCrawlerManager.Builder createManagerBuilder() {
        return CatalogCrawlerManager.Builder.newInstance()
                .configuration(enabledCrawler())
                .nodeDirectory(nodeDirectoryMock)
                .nodeQueryAdapterRegistry(crawlerActionRegistry)
                .store(store)
                .monitor(monitorMock);
    }

    @NotNull
    private TargetNode createNode() {
        return new TargetNode("testnode" + UUID.randomUUID(), "did:web:" + UUID.randomUUID(), "http://test.com", List.of(TEST_PROTOCOL));
    }

}
