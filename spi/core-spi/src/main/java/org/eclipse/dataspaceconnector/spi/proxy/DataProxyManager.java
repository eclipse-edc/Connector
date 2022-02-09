package org.eclipse.dataspaceconnector.spi.proxy;

import org.eclipse.dataspaceconnector.spi.system.Feature;

/**
 * This manager is used on the provider-side of a synchronous data transfer to maintain a list of {@link DataProxy} instances.
 */
@Feature(DataProxyManager.FEATURE)
public interface DataProxyManager {
    String FEATURE = "edc:core:transfer:dataproxymanager";

    void addProxy(String type, DataProxy proxy);

    /**
     * Gets a {@link DataProxy} for a particular data destination type. If none is found, {@code null} is returned.
     */
    DataProxy getProxy(String type);
}
