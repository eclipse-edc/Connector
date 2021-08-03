package org.eclipse.edc.policy.registry.memory;

import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.policy.PolicyRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory, threadsafe policy registry.
 *
 * This implementation is intended for testing purposes only.
 */
public class InMemoryPolicyRegistry implements PolicyRegistry {
    private Map<String, Policy> cache = new ConcurrentHashMap<>();

    @Override
    public @Nullable Policy resolvePolicy(String id) {
        return cache.get(id);
    }

    @Override
    public Collection<Policy> allPolicies() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public void registerPolicy(Policy policy) {
        cache.put(policy.getUid(), policy);
    }

    @Override
    public void removePolicy(String id) {
        cache.remove(id);
    }


}
