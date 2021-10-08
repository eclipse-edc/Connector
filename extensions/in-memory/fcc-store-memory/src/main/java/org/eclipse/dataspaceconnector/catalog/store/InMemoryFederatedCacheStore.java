package org.eclipse.dataspaceconnector.catalog.store;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An ephemeral in-memory cache store.
 */
public class InMemoryFederatedCacheStore implements FederatedCacheStore {

    private final Map<String, Asset> cache = new ConcurrentHashMap<>();

    @Override
    public void save(Asset asset) {
        cache.put(asset.getId(), asset);
    }

    @Override
    public Collection<Asset> query(CacheQuery query) {
        // to implement in a separated PR: filtering of the assets based on the query
        return cache.values();
    }

    @Override
    public Collection<Asset> getAll() {
        return cache.values();
    }
}
