package com.microsoft.dagx.iam.mock;

import com.microsoft.dagx.spi.iam.IdentityService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

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
        var region = context.getSetting("dagx.mock.region", "eu");
        context.registerService(IdentityService.class, new MockIdentityService(region));
        context.getMonitor().info("Initialized Mock IAM extension with region: " + region);
    }
}
