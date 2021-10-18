package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Collection;
import java.util.List;

public interface FederatedCacheStore {
    String FEATURE = "edc:catalog:cache:store";

    void save(Asset asset);

    Collection<Asset> query(List<Criterion> query);

    Collection<Asset> getAll();
}
