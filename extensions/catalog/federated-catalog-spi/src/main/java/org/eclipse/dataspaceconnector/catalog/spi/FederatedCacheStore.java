package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Collection;

public interface FederatedCacheStore {
    String FEATURE = "edc:catalog:cache:store";

    void save(Asset asset);

    Collection<Asset> query(CacheQuery query);

    Collection<Asset> getAll();
}
