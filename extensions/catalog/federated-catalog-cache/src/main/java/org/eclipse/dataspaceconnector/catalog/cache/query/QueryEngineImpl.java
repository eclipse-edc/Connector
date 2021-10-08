package org.eclipse.dataspaceconnector.catalog.cache.query;

import org.eclipse.dataspaceconnector.catalog.spi.QueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.QueryEngine;
import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class QueryEngineImpl implements QueryEngine {

    private final QueryAdapterRegistry queryAdapterRegistry;

    public QueryEngineImpl(QueryAdapterRegistry queryAdapterRegistry) {
        this.queryAdapterRegistry = queryAdapterRegistry;
    }

    @Override
    public Collection<Asset> getCatalog(CacheQuery query) {
        List<Asset> result = new ArrayList<>();
        queryAdapterRegistry.getAllAdapters()
                .forEach(queryAdapter -> result.addAll(queryAdapter.executeQuery(query).collect(Collectors.toList())));
        return result;
    }
}
