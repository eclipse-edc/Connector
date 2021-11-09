package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.FederatedCatalogCacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

/**
 * Accepts a {@link FederatedCatalogCacheQuery} and fetches a collection of {@link Asset} that conform to that query.
 */
@FunctionalInterface
public interface QueryEngine {
    String FEATURE = "edc:catalog:query:engine";

    QueryResponse getCatalog(FederatedCatalogCacheQuery query);
}
