/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.spi.types.domain.transfer;

public interface StatusCheckerRegistry {
    void register(Class<? extends ProvisionedResource> provisionedResourceClass, StatusChecker<? extends ProvisionedResource> statusChecker);

    <T extends ProvisionedResource> StatusChecker<T> resolve(Class<? extends ProvisionedResource> provisionedResourceClass);

    <T extends ProvisionedResource> StatusChecker<T> resolve(T resource);
}
