package com.microsoft.dagx.policy.registry.memory;

import com.microsoft.dagx.spi.policy.PolicyRegistry;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

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
