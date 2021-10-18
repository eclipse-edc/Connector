package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.QueryAdapterRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class InMemoryQueryAdapterRegistryExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of(QueryAdapterRegistry.FEATURE);
    }


    @Override
    public void initialize(ServiceExtensionContext context) {

        QueryAdapterRegistry registry = new InMemoryQueryAdapterRegistry();

        context.registerService(QueryAdapterRegistry.class, registry);
        context.getMonitor().info("Initialized In-Memory Query Adapter Registry");
    }
}
