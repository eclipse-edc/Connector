package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

public class InMemoryNodeDirectory implements FederatedCacheNodeDirectory {
    @Override
    public List<FederatedCacheNode> getAll() {
        return List.of(new FederatedCacheNode() {
            @Override
            public URL getUrl() {
                try {
                    return new URL("https://test.url1.com");
                } catch (MalformedURLException e) {
                    throw new EdcException(e);
                }
            }

            @Override
            public List<String> getSupportedProtocols() {
                return List.of("ids-rest");
            }
        });
    }

    @Override
    public Stream<FederatedCacheNode> getAllAsync() {
        return Stream.of(getAll().toArray(new FederatedCacheNode[0]));
    }
}
