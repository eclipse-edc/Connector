package org.eclipse.dataspaceconnector.iam.did.hub.memory;

import org.eclipse.dataspaceconnector.iam.did.hub.spi.IdentityHubStore;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 *
 */
public class InMemoryIdentityHubExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of("identity-hub-store");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var store = new InMemoryIdentityHubStore();
        context.registerService(IdentityHubStore.class, store);

        context.getMonitor().info("Initialized In-Memory Identity Hub extension");
    }
}
