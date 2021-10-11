package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapterRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class InMemoryProtocolAdapterRegistryExtension implements ServiceExtension {
    @Override
    public Set<String> provides() {
        return Set.of(ProtocolAdapterRegistry.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(ProtocolAdapterRegistry.class, new InMemoryProtocolAdapterRegistry());
        context.getMonitor().info("Initialized In-Memory Protocol Adapter Registry");
    }
}
