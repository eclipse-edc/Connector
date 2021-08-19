package org.eclipse.dataspaceconnector.iam.did.hub;

import org.eclipse.dataspaceconnector.iam.did.hub.spi.IdentityHub;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.IdentityHubStore;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 *
 */
public class CoreIdentityHubExtension implements ServiceExtension {

    @Override
    public Set<String> requires() {
        return Set.of("identity-hub-store");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var store = context.getService(IdentityHubStore.class);
        var hub = new IdentityHubImpl(store);

        var controller = new IdentityHubController();
        var webService = context.getService(WebService.class);
        webService.registerController(controller);

        context.registerService(IdentityHub.class, hub);

        context.getMonitor().info("Initialized Core Identity Hub extension");
    }

}
