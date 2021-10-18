package org.eclipse.dataspaceconnector.catalog.store;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.asset.CriterionConverter;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

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

    private final Map<String, Asset> cache = new ConcurrentHashMap<>();
    private final CriterionConverter<Predicate<Asset>> converter;

    public InMemoryFederatedCacheStore(CriterionConverter<Predicate<Asset>> converter) {
        this.converter = converter;
    }

    @Override
    public void save(Asset asset) {
        cache.put(asset.getId(), asset);
    }

    @Override
    public Collection<Asset> query(List<Criterion> query) {
        //AND all predicates
        var rootPredicate = query.stream().map(converter::convert).reduce(x -> true, Predicate::and);
        return cache.values().stream().filter(rootPredicate).collect(Collectors.toList());
    }

    @Override
    public Collection<Asset> getAll() {
        return cache.values();
    }

}
