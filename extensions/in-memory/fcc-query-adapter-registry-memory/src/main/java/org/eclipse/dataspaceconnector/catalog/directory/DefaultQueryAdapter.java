package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.catalog.spi.QueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.stream.Stream;

public class DefaultQueryAdapter implements QueryAdapter {

    private final FederatedCacheStore store;

    public DefaultQueryAdapter(FederatedCacheStore store) {
        this.store = store;
    }

    @Override
    public Stream<Asset> executeQuery(CacheQuery query) {
        return store.query(query).stream();
    }

    @Override
    public boolean canExecute(CacheQuery query) {
        return true; //todo: implement this when the CacheQuery is implemented
    }
}
