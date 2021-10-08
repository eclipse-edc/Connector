package org.eclipse.dataspaceconnector.catalog.spi;

import java.util.Collection;

/**
 * Registry where {@link QueryAdapter} instances are stored and maintained.
 */
public interface QueryAdapterRegistry {
    String FEATURE = "edc:catalog:cache:query:registry";

    /**
     * Finds all instances of {@link QueryAdapter} that was registered.
     *
     * @return The full list of query adapters that have been registered.
     */
    Collection<QueryAdapter> getAllAdapters();

    /**
     * Registers a {@link QueryAdapter} for a given storage type
     */
    void register(QueryAdapter adapter);
}
