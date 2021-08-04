/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.policy.registry.memory;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory, threadsafe policy registry.
 * <p>
 * This implementation is intended for testing purposes only.
 */
public class InMemoryPolicyRegistry implements PolicyRegistry {
    private final Map<String, Policy> cache = new ConcurrentHashMap<>();

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
