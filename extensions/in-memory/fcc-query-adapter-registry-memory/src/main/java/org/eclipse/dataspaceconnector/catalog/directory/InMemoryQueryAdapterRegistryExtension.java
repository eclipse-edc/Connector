package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.catalog.spi.QueryAdapterRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class InMemoryQueryAdapterRegistryExtension implements ServiceExtension {
    
    @Override
    public Set<String> requires() {
        return Set.of(FederatedCacheStore.FEATURE);
    }

    @Override
    public Set<String> provides() {
        return Set.of(QueryAdapterRegistry.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        FederatedCacheStore store = context.getService(FederatedCacheStore.class);

        QueryAdapterRegistry registry = new InMemoryQueryAdapterRegistry();
        registry.register(new InMemoryQueryAdapter(store));
        context.registerService(QueryAdapterRegistry.class, registry);
        context.getMonitor().info("Initialized In-Memory Query Adapter Registry");
    }
}
