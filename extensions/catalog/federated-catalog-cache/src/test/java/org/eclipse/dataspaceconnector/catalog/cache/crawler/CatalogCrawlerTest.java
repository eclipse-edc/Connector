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

package org.eclipse.dataspaceconnector.catalog.cache.crawler;

import org.eclipse.dataspaceconnector.catalog.spi.CrawlerErrorHandler;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.catalog.cache.TestUtil.createCatalog;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CatalogCrawlerTest {

    private CatalogCrawler crawler;
    private CrawlerErrorHandler errorHandlerMock;

    @BeforeEach
    void setUp() {
        errorHandlerMock = mock(CrawlerErrorHandler.class);
        crawler = new CatalogCrawler(mock(Monitor.class), errorHandlerMock);
    }

    @Test
    void run() {
        WorkItem target = createWorkItem();
        var adapter = new NodeQueryAdapter() {
            @Override
            public CompletableFuture<UpdateResponse> sendRequest(UpdateRequest request) {
                return CompletableFuture.completedFuture(new UpdateResponse(target.getUrl(), createCatalog()));
            }
        };
        assertThat(crawler.run(target, adapter)).isCompleted();
        verifyNoInteractions(errorHandlerMock);
    }

    @Test
    void run_withError() {
        WorkItem target = createWorkItem();
        var adapter = new NodeQueryAdapter() {
            @Override
            public CompletableFuture<UpdateResponse> sendRequest(UpdateRequest request) {
                return CompletableFuture.failedFuture(new EdcException("foobar"));
            }
        };
        assertThat(crawler.run(target, adapter)).isCompletedExceptionally();
        verify(errorHandlerMock).accept(eq(target));
    }

    @Test
    void run_throwsException() {
        WorkItem target = createWorkItem();
        var adapter = mock(NodeQueryAdapter.class);
        when(adapter.sendRequest(any())).thenThrow(new RuntimeException("test exception"));
        assertThat(crawler.run(target, adapter)).isCompletedExceptionally();
        verify(errorHandlerMock).accept(eq(target));
    }

    @Test
    void getId() {
        assertThat(crawler.getId()).isNotNull();
    }


    @NotNull
    private WorkItem createWorkItem() {
        return new WorkItem("test-url", "test-protocol");
    }
}