/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.core;

import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import com.microsoft.dagx.spi.types.domain.transfer.StatusChecker;
import com.microsoft.dagx.spi.types.domain.transfer.StatusCheckerRegistry;

import java.util.HashMap;
import java.util.Map;

public class StatusCheckerRegistryImpl implements StatusCheckerRegistry {
    private final Map<Class<? extends ProvisionedResource>, StatusChecker<? extends ProvisionedResource>> inMemoryMap;

    public StatusCheckerRegistryImpl() {
        inMemoryMap = new HashMap<>();
    }

    @Override
    public void register(Class<? extends ProvisionedResource> provisionedResourceClass, StatusChecker<? extends ProvisionedResource> statusChecker) {
        inMemoryMap.put(provisionedResourceClass, statusChecker);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ProvisionedResource> StatusChecker<T> resolve(Class<? extends ProvisionedResource> provisionedResourceClass) {
        return (StatusChecker<T>) inMemoryMap.get(provisionedResourceClass);
    }

    @Override
    public <T extends ProvisionedResource> StatusChecker<T> resolve(T resource) {
        return resolve(resource.getClass());
    }
}
