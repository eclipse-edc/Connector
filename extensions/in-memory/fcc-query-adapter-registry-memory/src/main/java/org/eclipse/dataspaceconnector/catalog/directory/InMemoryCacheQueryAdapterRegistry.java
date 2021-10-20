package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.QueryResponse;
import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class InMemoryCacheQueryAdapterRegistry implements CacheQueryAdapterRegistry {

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
    public QueryResponse executeQuery(CacheQuery query) {

        var adapters = registry.stream().filter(ad -> ad.canExecute(query)).collect(Collectors.toList());

        if (adapters.isEmpty()) {
            return QueryResponse.Builder.newInstance()
                    .status(QueryResponse.Status.NO_ADAPTER_FOUND)
                    .build();

        }

        var responseBuilder = QueryResponse.Builder.newInstance()
                .status(QueryResponse.Status.ACCEPTED);
        Stream<Asset> assets = Stream.empty();

        // add the results of all query adapters to the union stream
        for (var adapter : adapters) {
            try {
                assets = Stream.concat(assets, adapter.executeQuery(query));
            } catch (EdcException ex) {
                responseBuilder.error("Adapter failed: " + ex.getMessage());
            }
        }

        return responseBuilder.assets(assets).build();
    }
}
