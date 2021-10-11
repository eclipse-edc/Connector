package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class InMemoryNodeDirectoryExtension implements ServiceExtension {
    @Override
    public Set<String> provides() {
        return Set.of(FederatedCacheNodeDirectory.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(FederatedCacheNodeDirectory.class, new InMemoryNodeDirectory());
        context.getMonitor().info("Initialized In-Memory Node Directory");
    }
}
