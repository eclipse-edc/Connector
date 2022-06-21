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

import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.QueryEngine;
import org.eclipse.dataspaceconnector.catalog.spi.QueryResponse;
import org.eclipse.dataspaceconnector.catalog.spi.model.FederatedCatalogCacheQuery;

public class QueryEngineImpl implements QueryEngine {

    private final CacheQueryAdapterRegistry cacheQueryAdapterRegistry;

    public QueryEngineImpl(CacheQueryAdapterRegistry cacheQueryAdapterRegistry) {
        this.cacheQueryAdapterRegistry = cacheQueryAdapterRegistry;
    }

    @Override
    public QueryResponse getCatalog(FederatedCatalogCacheQuery query) {
        return cacheQueryAdapterRegistry.executeQuery(query);
    }
}
