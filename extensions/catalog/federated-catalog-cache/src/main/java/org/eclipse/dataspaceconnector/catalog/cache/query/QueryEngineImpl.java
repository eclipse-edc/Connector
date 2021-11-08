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
