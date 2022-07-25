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
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

public class CatalogCrawler {
    private final Monitor monitor;
    private final CrawlerErrorHandler errorHandler;
    private final String crawlerId;

    public CatalogCrawler(Monitor monitor, @NotNull CrawlerErrorHandler errorHandler) {
        this.monitor = monitor;
        this.errorHandler = errorHandler;
        crawlerId = format("Crawler-%s", UUID.randomUUID());
    }

    public CompletableFuture<UpdateResponse> run(WorkItem target, @NotNull NodeQueryAdapter adapter) {
        try {
            monitor.debug(format("%s: WorkItem acquired", crawlerId));
            var updateFuture = adapter.sendRequest(new UpdateRequest(target.getUrl()));
            return updateFuture.whenComplete((updateResponse, throwable) -> {
                if (throwable != null) {
                    handleError(target, throwable.getMessage());
                }
            });
        } catch (Throwable thr) {
            handleError(target, thr.getMessage());
            return CompletableFuture.failedFuture(new EdcException(thr));
        }
    }

    public String getId() {
        return crawlerId;
    }

    private void handleError(@Nullable WorkItem errorWorkItem, String message) {
        monitor.severe(message);
        if (errorWorkItem != null) {
            errorWorkItem.error(message);
            errorHandler.accept(errorWorkItem);
        }
    }

}
