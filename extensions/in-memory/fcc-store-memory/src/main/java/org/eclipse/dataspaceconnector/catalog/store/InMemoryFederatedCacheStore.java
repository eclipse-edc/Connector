package org.eclipse.dataspaceconnector.catalog.store;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.CriterionConverter;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

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

    private final Map<String, ContractOffer> cache = new ConcurrentHashMap<>();
    private final CriterionConverter<Predicate<ContractOffer>> converter;

    public InMemoryFederatedCacheStore(CriterionConverter<Predicate<ContractOffer>> converter) {
        this.converter = converter;
    }

    @Override
    public void save(ContractOffer contractOffer) {
        cache.put(contractOffer.getAsset().getId(), contractOffer);
    }

    @Override
    public Collection<ContractOffer> query(List<Criterion> query) {
        //AND all predicates
        var rootPredicate = query.stream().map(converter::convert).reduce(x -> true, Predicate::and);
        return cache.values().stream().filter(rootPredicate).collect(Collectors.toList());
    }
}
