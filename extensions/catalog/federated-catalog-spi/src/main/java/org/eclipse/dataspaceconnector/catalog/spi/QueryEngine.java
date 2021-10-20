package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

/**
 * Accepts a {@link CacheQuery} and fetches a collection of {@link Asset} that conform to that query.
 */
@FunctionalInterface
public interface QueryEngine {
    QueryResponse getCatalog(CacheQuery query);
}
