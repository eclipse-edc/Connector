package org.eclipse.dataspaceconnector.catalog.spi;

import java.util.List;

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
     * Inserts (="registers") a node into the directory.
     */
    void insert(FederatedCacheNode node);
}
