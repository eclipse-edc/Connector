package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.spi.asset.Criterion;

import java.util.Collection;
import java.util.List;

/**
 * Internal datastore where all the catalogs from all the other connectors are stored by the FederatedCatalogCache.
 */
public interface FederatedCacheStore {
    String FEATURE = "edc:catalog:cache:store";

    /**
     * todo: rename _this_ asset to something else, and add the originator as property
     * Adds an {@link CachedAsset} to the store
     */
    void save(CachedAsset asset);

    /**
     * Queries the store for {@link CachedAsset}s
     *
     * @param query A list of criteria the asset must fulfill
     * @return A collection of assets that are already in the store and that satisfy a given list of criteria.
     */
    Collection<CachedAsset> query(List<Criterion> query);

}