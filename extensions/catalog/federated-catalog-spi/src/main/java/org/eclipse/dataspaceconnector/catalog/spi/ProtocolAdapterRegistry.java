package org.eclipse.dataspaceconnector.catalog.spi;

import java.util.Collection;

/**
 * Registry where {@link ProtocolAdapter} instances are stored and maintained.
 */
public interface ProtocolAdapterRegistry {
    String FEATURE = "edc:catalog:cache:protocol:registry";

    /**
     * Finds a {@link ProtocolAdapter} that was registered for the given protocol name.
     *
     * @param protocolName An arbitrary String identifying the protocol.
     * @return A list of protocol adapters that can handle that protocol, or an empty list if none was found.
     */
    Collection<ProtocolAdapter> findForProtocol(String protocolName);

    /**
     * Registers a {@code ProtocolAdapter} for a given protocol
     */
    void register(String protocolName, ProtocolAdapter adapter);

    /**
     * Removes a specific {@code ProtocolAdapter} registration
     */
    void unregister(String protocolName, ProtocolAdapter adapter);
}
