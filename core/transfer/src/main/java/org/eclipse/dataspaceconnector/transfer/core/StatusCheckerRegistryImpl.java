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

package org.eclipse.dataspaceconnector.transfer.core;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusChecker;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;

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
