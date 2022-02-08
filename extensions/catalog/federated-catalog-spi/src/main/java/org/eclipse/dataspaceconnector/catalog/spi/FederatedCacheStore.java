package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import java.util.Collection;
import java.util.List;

/**
 * Internal datastore where all the catalogs from all the other connectors are stored by the FederatedCatalogCache.
 */
@Feature(FederatedCacheStore.FEATURE)
public interface FederatedCacheStore {
    String FEATURE = "edc:catalog:cache:store";

    /**
     * Adds an {@link ContractOffer} to the store
     */
    void save(ContractOffer asset);

    /**
     * Queries the store for {@link ContractOffer}s
     *
     * @param query A list of criteria the asset must fulfill
     * @return A collection of assets that are already in the store and that satisfy a given list of criteria.
     */
    Collection<ContractOffer> query(List<Criterion> query);

}