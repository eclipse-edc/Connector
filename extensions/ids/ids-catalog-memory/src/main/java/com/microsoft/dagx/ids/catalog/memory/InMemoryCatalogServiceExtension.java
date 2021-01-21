package com.microsoft.dagx.ids.catalog.memory;

import com.microsoft.dagx.ids.spi.catalog.CatalogService;
import com.microsoft.dagx.ids.spi.catalog.CatalogServiceExtension;

/**
 * Provides a simple, in-memory implementation of an IDS Catalog intended for testing.
 */
public class InMemoryCatalogServiceExtension implements CatalogServiceExtension {

    @Override
    public CatalogService getCatalogService() {
        return null;
    }
}
