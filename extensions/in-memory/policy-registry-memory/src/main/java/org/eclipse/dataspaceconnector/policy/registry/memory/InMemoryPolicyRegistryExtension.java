package org.eclipse.dataspaceconnector.policy.registry.memory;

import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 *
 */
public class InMemoryPolicyRegistryExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of("policy-registry");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(PolicyRegistry.class, new InMemoryPolicyRegistry());
        context.getMonitor().info("Initialized In-Memory Policy Registry extension");
    }

}
