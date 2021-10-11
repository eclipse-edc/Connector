package org.eclipse.dataspaceconnector.catalog.spi;

import java.net.URL;
import java.util.List;

public interface FederatedCacheNode {
    URL getUrl();

    List<String> getSupportedProtocols();
}
