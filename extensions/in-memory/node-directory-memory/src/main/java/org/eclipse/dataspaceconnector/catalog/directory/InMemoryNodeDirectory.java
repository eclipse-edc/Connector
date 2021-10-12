package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class InMemoryNodeDirectory implements FederatedCacheNodeDirectory {
    @Override
    public List<FederatedCacheNode> getAll() {
        return IntStream.range(0, 10).mapToObj(i -> getNode(i)).collect(Collectors.toList());
    }

    @NotNull
    private FederatedCacheNode getNode(int number) {
        return new FederatedCacheNode() {
            @Override
            public URL getUrl() {
                try {
                    return new URL("https://test.url1.com/" + number);
                } catch (MalformedURLException e) {
                    throw new EdcException(e);
                }
            }

            @Override
            public List<String> getSupportedProtocols() {
                return List.of("ids-rest");
            }
        };
    }

    @Override
    public Stream<FederatedCacheNode> getAllAsync() {
        return Stream.of(getAll().toArray(new FederatedCacheNode[0]));
    }
}
