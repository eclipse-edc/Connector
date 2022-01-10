package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.spi.system.Feature;

import java.util.Collection;

/**
 * Registry where {@link NodeQueryAdapter} instances are stored and maintained.
 */
@Feature(NodeQueryAdapterRegistry.FEATURE)
public interface NodeQueryAdapterRegistry {
    String FEATURE = "edc:catalog:cache:protocol:registry";

    /**
     * Finds a {@link NodeQueryAdapter} that was registered for the given protocol name.
     *
     * @param protocolName An arbitrary String identifying the protocol.
     * @return A list of protocol adapters that can handle that protocol, or an empty list if none was found.
     */
    Collection<NodeQueryAdapter> findForProtocol(String protocolName);

    /**
     * Registers a {@link NodeQueryAdapter} for a given protocol
     */
    void register(String protocolName, NodeQueryAdapter adapter);

    /**
     * Removes a specific {@link NodeQueryAdapter} registration
     */
    void unregister(String protocolName, NodeQueryAdapter adapter);
}
