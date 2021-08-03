package org.eclipse.edc.policy.registry.memory;

import org.eclipse.edc.spi.policy.PolicyRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

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
