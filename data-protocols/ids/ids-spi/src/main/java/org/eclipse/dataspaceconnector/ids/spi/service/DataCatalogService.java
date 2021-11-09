package org.eclipse.dataspaceconnector.ids.spi.service;

import org.eclipse.dataspaceconnector.ids.spi.types.DataCatalog;

/**
 * The IDS service is able to create a description of the EDC data catalog.
 */
public interface DataCatalogService {

    /**
     * Provides the data catalog
     *
     * @return data catalog
     */
    DataCatalog getDataCatalog();
}
