package org.eclipse.dataspaceconnector.catalog.spi;

import java.util.List;
import java.util.stream.Stream;

public interface FederatedCacheNodeDirectory {
    String FEATURE = "edc:catalog:node-directory";

    List<FederatedCacheNode> getAll();

    Stream<FederatedCacheNode> getAllAsync();

    void insert(FederatedCacheNode node);
}
