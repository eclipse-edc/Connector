package org.eclipse.dataspaceconnector.catalog.store;

import org.eclipse.dataspaceconnector.catalog.spi.CachedAsset;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.asset.CriterionConverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An ephemeral in-memory cache store.
 */
public class InMemoryFederatedCacheStore implements FederatedCacheStore {

    private final Map<String, CachedAsset> cache = new ConcurrentHashMap<>();
    private final CriterionConverter<Predicate<CachedAsset>> converter;

    public InMemoryFederatedCacheStore(CriterionConverter<Predicate<CachedAsset>> converter) {
        this.converter = converter;
    }

    @Override
    public void save(CachedAsset asset) {
        cache.put(asset.getId(), asset);
    }

    @Override
    public Collection<CachedAsset> query(List<Criterion> query) {
        //AND all predicates
        var rootPredicate = query.stream().map(converter::convert).reduce(x -> true, Predicate::and);
        return cache.values().stream().filter(rootPredicate).collect(Collectors.toList());
    }

}
