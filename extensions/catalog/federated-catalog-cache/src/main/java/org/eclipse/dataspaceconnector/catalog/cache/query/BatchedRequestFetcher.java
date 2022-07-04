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

import java.util.ArrayList;
import java.util.List;


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
    public List<ContractOffer> fetch(CatalogRequest catalogRequest, int from, int batchSize) {
        int fetched;
        List<ContractOffer> allOffers = new ArrayList<>();
        var to = from + batchSize;

        do {

            Catalog catalog = getCatalog(catalogRequest, from, to);
            fetched = catalog.getContractOffers().size();
            allOffers.addAll(catalog.getContractOffers());
            to += batchSize;
            from += batchSize;
        } while (fetched >= batchSize);

        return allOffers;
    }

    private Catalog getCatalog(CatalogRequest catalogRequest, int from, int to) {
        var future = dispatcherRegistry.send(Catalog.class, catalogRequest.toBuilder().range(new Range(from, to)).build(), () -> null);
        return future.join();
    }

}
