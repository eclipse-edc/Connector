/*
 * Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *      Microsoft Corporation - initial API and implementation
 *
 *
 */

package org.eclipse.dataspaceconnector.catalog.cache.query;

import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.catalog.spi.model.FederatedCatalogCacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class CacheQueryAdapterImpl implements CacheQueryAdapter {

    private final FederatedCacheStore store;

    public CacheQueryAdapterImpl(FederatedCacheStore store) {
        this.store = store;
    }

    @Override
    public @NotNull Stream<ContractOffer> executeQuery(FederatedCatalogCacheQuery query) {
        //todo: translate the generic CacheQuery into a list of criteria and
        return store.query(query.getCriteria()).stream();
    }

    @Override
    public boolean canExecute(FederatedCatalogCacheQuery query) {
        return true; //todo: implement this when the CacheQuery is implemented
    }
}
