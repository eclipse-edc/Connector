package org.eclipse.dataspaceconnector.catalog.spi;

import java.util.Collection;

/**
 * Registry where {@link CatalogQueryAdapter} instances are stored and maintained.
 */
public interface CatalogQueryAdapterRegistry {
    String FEATURE = "edc:catalog:cache:protocol:registry";

    /**
     * Finds a {@link CatalogQueryAdapter} that was registered for the given protocol name.
     *
     * @param protocolName An arbitrary String identifying the protocol.
     * @return A list of protocol adapters that can handle that protocol, or an empty list if none was found.
     */
    Collection<CatalogQueryAdapter> findForProtocol(String protocolName);

    /**
     * Registers a {@code ProtocolAdapter} for a given protocol
     */
    void register(String protocolName, CatalogQueryAdapter adapter);

    /**
     * Removes a specific {@code ProtocolAdapter} registration
     */
    void unregister(String protocolName, CatalogQueryAdapter adapter);
}
