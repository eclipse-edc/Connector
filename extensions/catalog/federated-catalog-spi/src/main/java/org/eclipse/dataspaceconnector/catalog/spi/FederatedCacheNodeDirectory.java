package org.eclipse.dataspaceconnector.catalog.spi;

import java.util.List;
import java.util.stream.Stream;

/**
 * A global list of all FederatedCacheNodes that are available in a data space, much like a "phone book" for catalog endpoints.
 */
public interface FederatedCacheNodeDirectory {
    String FEATURE = "edc:catalog:node-directory";

    /**
     * Get all nodes.
     */
    List<FederatedCacheNode> getAll();

    /**
     * Get all nodes asynchronously. Useful if the backing storage system has an async API.
     * By default this just returns {@code Stream.of(getAll())}.
     */
    default Stream<FederatedCacheNode> getAllAsync(){
        return getAll().stream();
    }

    /**
     * Inserts (="registers") a node into the directory.
     */
    void insert(FederatedCacheNode node);
}
