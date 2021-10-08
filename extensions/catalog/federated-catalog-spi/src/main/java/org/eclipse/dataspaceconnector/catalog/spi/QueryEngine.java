package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Collection;

public interface QueryEngine {
    Collection<Asset> getCatalog(CacheQuery query);
}
