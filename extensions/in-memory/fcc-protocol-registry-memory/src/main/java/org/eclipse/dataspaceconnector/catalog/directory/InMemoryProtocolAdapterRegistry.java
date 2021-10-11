package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapterRegistry;

import java.util.Collection;

class InMemoryProtocolAdapterRegistry implements ProtocolAdapterRegistry {

    @Override
    public Collection<ProtocolAdapter> findForProtocol(String protocolName) {
        return null;
    }

    @Override
    public void register(String protocolName, ProtocolAdapter adapter) {

    }

    @Override
    public void unregister(String protocolName, ProtocolAdapter adapter) {

    }
}
