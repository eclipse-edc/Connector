package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;

import java.util.List;
import java.util.stream.Stream;

public class InMemoryNodeDirectory implements FederatedCacheNodeDirectory {
    @Override
    public List<FederatedCacheNode> getAll() {
        return null;
    }

    @Override
    public Stream<FederatedCacheNode> getAllAsync() {
        return null;
    }
}
