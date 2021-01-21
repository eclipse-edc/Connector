package com.microsoft.dagx.ids.spi.catalog;

/**
 * Provides an implementation of an IDS Catalog. This may delegate to an external system such as a federate catalog.
 */
public interface CatalogServiceExtension {

    /**
     * Starts the extension.
     */
    default void start() {
    }

    /**
     * Stop the extension and dispose of resources.
     */
    default void stop() {
    }

    /**
     * Returns the catalog service.
     */
    CatalogService getCatalogService();

}
