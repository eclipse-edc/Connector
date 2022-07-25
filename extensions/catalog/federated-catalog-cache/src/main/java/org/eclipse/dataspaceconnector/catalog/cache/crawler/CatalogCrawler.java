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

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.lang.String.format;

public class CatalogCrawler {
    private final Monitor monitor;
    private final CrawlerErrorHandler errorHandler;
    private final String crawlerId;
    private final Consumer<UpdateResponse> successHandler;

    public CatalogCrawler(Monitor monitor, @NotNull CrawlerErrorHandler errorHandler, @Nullable Consumer<UpdateResponse> successHandler) {
        this.monitor = monitor;
        this.errorHandler = errorHandler;
        this.successHandler = successHandler;
        crawlerId = format("Crawler-%s", UUID.randomUUID());
    }

    public CompletableFuture<Void> run(WorkItem target, Collection<NodeQueryAdapter> adapters) {
        try {
            monitor.debug(format("%s: WorkItem acquired", crawlerId));

            // search for an adapter

            if (adapters.isEmpty()) {
                // otherwise error out the workitem
                handleError(target, format("%s: No Adapter found for protocol [%s :: %s]", crawlerId, target.getProtocol(), target.getUrl()));
                return CompletableFuture.completedFuture(null);
            } else {
                var allFutures = new ArrayList<CompletableFuture<UpdateResponse>>();
                // if the adapters are found, use them to send the update request
                for (NodeQueryAdapter a : adapters) {
                    var updateFuture = a.sendRequest(new UpdateRequest(target.getUrl()));
                    allFutures.add(updateFuture);
                    updateFuture.whenComplete((updateResponse, throwable) -> {
                        if (throwable != null) {
                            handleError(target, throwable.getMessage());
                        } else {
                            handleResponse(updateResponse);
                        }
                    });
                }
                return CompletableFuture.allOf(allFutures.toArray(CompletableFuture[]::new));

            }
        } catch (Throwable thr) {
            //runnables that run on an executor may swallow the exception
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

    private void handleResponse(UpdateResponse updateResponse) {
        if (successHandler != null) {
            successHandler.accept(updateResponse);
        }
    }
}
