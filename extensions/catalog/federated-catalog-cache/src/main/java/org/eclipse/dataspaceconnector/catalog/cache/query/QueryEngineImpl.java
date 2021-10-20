package org.eclipse.dataspaceconnector.catalog.cache.query;

import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.QueryEngine;
import org.eclipse.dataspaceconnector.catalog.spi.QueryResponse;
import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;

public class QueryEngineImpl implements QueryEngine {

    private final CacheQueryAdapterRegistry cacheQueryAdapterRegistry;

    public QueryEngineImpl(CacheQueryAdapterRegistry cacheQueryAdapterRegistry) {
        this.cacheQueryAdapterRegistry = cacheQueryAdapterRegistry;
    }

    @Override
    public QueryResponse getCatalog(CacheQuery query) {
        // todo: the following code must be moved to the API, e.g. a REST controller:
        //        if (queryResponse.getStatus() == QueryResponse.Status.NO_ADAPTER_FOUND) {
        //            throw new QueryNotAcceptedException();
        //        }
        //        if (!queryResponse.getErrors().isEmpty()) {
        //            throw new QueryException(queryResponse.getErrors());
        //        }
        return cacheQueryAdapterRegistry.executeQuery(query);
    }
}
