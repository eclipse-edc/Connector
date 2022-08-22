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

package org.eclipse.dataspaceconnector.catalog.cache.query;

import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.QueryResponse;
import org.eclipse.dataspaceconnector.catalog.spi.model.FederatedCatalogCacheQuery;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CacheQueryAdapterRegistryImpl implements CacheQueryAdapterRegistry {

    private final Set<CacheQueryAdapter> registry = new CopyOnWriteArraySet<>();

    @Override
    public Collection<CacheQueryAdapter> getAllAdapters() {
        return new ArrayList<>(registry);
    }

    @Override
    public void register(CacheQueryAdapter adapter) {
        registry.add(adapter);
    }

    @Override
    public QueryResponse executeQuery(FederatedCatalogCacheQuery query) {

        var adapters = registry.stream().filter(ad -> ad.canExecute(query)).collect(Collectors.toList());

        if (adapters.isEmpty()) {
            return QueryResponse.Builder.newInstance()
                    .status(QueryResponse.Status.NO_ADAPTER_FOUND)
                    .build();

        }

        var responseBuilder = QueryResponse.Builder.newInstance()
                .status(QueryResponse.Status.ACCEPTED);
        Stream<ContractOffer> offers = Stream.empty();

        // add the results of all query adapters to the union stream
        for (var adapter : adapters) {
            try {
                offers = Stream.concat(offers, adapter.executeQuery(query));
            } catch (EdcException ex) {
                responseBuilder.error("Adapter failed: " + ex.getMessage());
            }
        }

        return responseBuilder.offers(offers.collect(Collectors.toList())).build();
    }
}
