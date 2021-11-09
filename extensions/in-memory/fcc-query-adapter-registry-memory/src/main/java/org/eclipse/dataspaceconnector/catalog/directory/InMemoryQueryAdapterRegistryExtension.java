package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class InMemoryQueryAdapterRegistryExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of(CacheQueryAdapterRegistry.FEATURE);
    }


    @Override
    public void initialize(ServiceExtensionContext context) {

        CacheQueryAdapterRegistry registry = new InMemoryCacheQueryAdapterRegistry();

        context.registerService(CacheQueryAdapterRegistry.class, registry);
        context.getMonitor().info("Initialized In-Memory Query Adapter Registry");
    }
}
