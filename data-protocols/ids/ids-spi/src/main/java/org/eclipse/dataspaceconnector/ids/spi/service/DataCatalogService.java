package org.eclipse.dataspaceconnector.ids.spi.service;

import org.eclipse.dataspaceconnector.ids.spi.types.DataCatalog;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.jetbrains.annotations.NotNull;

/**
 * The IDS service is able to create a description of the EDC data catalog.
 */
public interface DataCatalogService {

    /**
     * Provides the data catalog
     *
     * @return data catalog
     */
    @NotNull
    DataCatalog getDataCatalog(VerificationResult verificationResult);
}
