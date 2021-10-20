package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Collection;
import java.util.List;

/**
 * Internal datastore where all the catalogs from all the other connectors are stored by the FederatedCatalogCache.
 */
public interface FederatedCacheStore {
    String FEATURE = "edc:catalog:cache:store";

    /**
     * Adds an {@link Asset} to the store
     */
    void save(Asset asset);

    /**
     * Queries the store for {@link Asset}s
     *
     * @param query A list of criteria the asset must fulfill
     * @return A collection of assets that are already in the store and that satisfy a given list of criteria.
     */
    Collection<Asset> query(List<Criterion> query);

    Collection<Asset> getAll();
}
