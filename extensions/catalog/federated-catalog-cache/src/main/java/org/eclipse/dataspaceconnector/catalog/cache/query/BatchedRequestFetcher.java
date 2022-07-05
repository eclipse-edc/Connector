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

package org.eclipse.dataspaceconnector.catalog.cache.query;

import org.eclipse.dataspaceconnector.spi.message.Range;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.CatalogRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Helper class that runs through a loop and sends {@link CatalogRequest}s until no more {@link ContractOffer}s are
 * received. This is useful to avoid overloading the provider connector by chunking the resulting response payload
 * size.
 */
public class BatchedRequestFetcher {
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;

    public BatchedRequestFetcher(RemoteMessageDispatcherRegistry dispatcherRegistry) {
        this.dispatcherRegistry = dispatcherRegistry;
    }

    /**
     * Gets all contract offers. Requests are split in digestible chunks to match {@code batchSize} until no more offers
     * can be obtained.
     *
     * @param catalogRequest The catalog request. This will be copied for every request.
     * @param from The (zero-based) index of the first item
     * @param batchSize The size of one batch
     * @return A list of {@link ContractOffer} objects
     */
    @NotNull
    public CompletableFuture<List<ContractOffer>> fetch(CatalogRequest catalogRequest, int from, int batchSize) {
        var range = new Range(from, from + batchSize);
        var rq = catalogRequest.toBuilder().range(range).build();

        return dispatcherRegistry.send(Catalog.class, rq, () -> null)
                .thenApply(Catalog::getContractOffers)
                .thenCompose(offers -> {
                    if (offers.size() > 0) {
                        return fetch(rq, range.getFrom() + batchSize, batchSize)
                                .thenApply(o -> concat(offers, o));
                    } else {
                        return CompletableFuture.completedFuture(offers);
                    }
                });
    }

    private List<ContractOffer> concat(List<ContractOffer> list1, List<ContractOffer> list2) {
        list1.addAll(list2);
        return list1;
    }


    private Catalog getCatalog(CatalogRequest catalogRequest, int from, int to) {
        var future = dispatcherRegistry.send(Catalog.class, catalogRequest.toBuilder().range(new Range(from, to)).build(), () -> null);
        return future.join();
    }

}
