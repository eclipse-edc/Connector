package org.eclipse.dataspaceconnector.iam.mock;

import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 * An IAM provider mock used for testing.
 */
public class IamMockExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of("iam");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var region = context.getSetting("dataspaceconnector.mock.region", "eu");
        context.registerService(IdentityService.class, new MockIdentityService(region));
        context.getMonitor().info("Initialized Mock IAM extension with region: " + region);
    }
}
