package org.eclipse.dataspaceconnector.catalog.cache.query;

import org.eclipse.dataspaceconnector.catalog.spi.QueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.QueryEngine;
import org.eclipse.dataspaceconnector.catalog.spi.QueryResponse;
import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Collection;
import java.util.stream.Collectors;

public class QueryEngineImpl implements QueryEngine {

    private final QueryAdapterRegistry queryAdapterRegistry;

    public QueryEngineImpl(QueryAdapterRegistry queryAdapterRegistry) {
        this.queryAdapterRegistry = queryAdapterRegistry;
    }

    @Override
    public Collection<Asset> getCatalog(CacheQuery query) {
        QueryResponse queryResponse = queryAdapterRegistry.executeQuery(query);

        // query not possible
        if (queryResponse.getStatus() == QueryResponse.Status.NO_ADAPTER_FOUND) {
            throw new QueryNotAcceptedException();
        }
        if (!queryResponse.getErrors().isEmpty()) {
            throw new QueryException(queryResponse.getErrors());
        }

        return queryResponse.getAssets().collect(Collectors.toList());
    }
}
