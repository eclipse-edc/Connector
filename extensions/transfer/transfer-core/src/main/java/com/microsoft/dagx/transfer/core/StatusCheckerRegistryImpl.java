/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.core;

import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import com.microsoft.dagx.spi.types.domain.transfer.StatusChecker;
import com.microsoft.dagx.spi.types.domain.transfer.StatusCheckerRegistry;

import java.util.HashMap;
import java.util.Map;

public class StatusCheckerRegistryImpl implements StatusCheckerRegistry {
    private final Map<Class<? extends ProvisionedDataDestinationResource>, StatusChecker<? extends ProvisionedDataDestinationResource>> inMemoryMap;

    public StatusCheckerRegistryImpl() {
        inMemoryMap = new HashMap<>();
    }

    @Override
    public void register(Class<? extends ProvisionedDataDestinationResource> provisionedResourceClass, StatusChecker<? extends ProvisionedDataDestinationResource> statusChecker) {
        inMemoryMap.put(provisionedResourceClass, statusChecker);
    }

    @Override
    public <T extends ProvisionedDataDestinationResource> StatusChecker<T> resolve(Class<? extends ProvisionedDataDestinationResource> provisionedResourceClass) {
        return (StatusChecker<T>) inMemoryMap.get(provisionedResourceClass);
    }

    @Override
    public <T extends ProvisionedDataDestinationResource> StatusChecker<T> resolve(T resource) {
        return resolve(resource.getClass());
    }
}
