package org.eclipse.dataspaceconnector.catalog.store;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class InMemoryFederatedCacheStoreExtension implements ServiceExtension {
    @Override
    public Set<String> provides() {
        return Set.of(FederatedCacheStore.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(FederatedCacheStore.class, new InMemoryFederatedCacheStore());
        context.getMonitor().info("Initialized In-Memory Federated Cache Store");
    }
}
