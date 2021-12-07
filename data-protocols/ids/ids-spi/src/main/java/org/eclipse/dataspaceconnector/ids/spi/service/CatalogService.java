package org.eclipse.dataspaceconnector.ids.spi.service;

import org.eclipse.dataspaceconnector.spi.Result;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.jetbrains.annotations.NotNull;

/**
 * The IDS service is able to create a description of the EDC data catalog.
 */
public interface CatalogService {

    /**
     * Provides the data catalog
     *
     * @return data catalog
     */
    @NotNull
    Catalog getDataCatalog(Result<ClaimToken> verificationResult);
}
