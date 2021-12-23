package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(FederatedCacheNodeDirectory.class)
public class InMemoryNodeDirectoryExtension implements ServiceExtension {

    @Override
    public String name() {
        return "In-Memory Node Directory";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(FederatedCacheNodeDirectory.class, new InMemoryNodeDirectory());
    }
}
