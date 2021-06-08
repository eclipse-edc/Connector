/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.spi.types.domain.transfer;

public interface StatusCheckerRegistry {
    void register(Class<? extends ProvisionedDataDestinationResource> provisionedResourceClass, StatusChecker<? extends ProvisionedDataDestinationResource> statusChecker);

    <T extends ProvisionedDataDestinationResource> StatusChecker<T> resolve(Class<? extends ProvisionedDataDestinationResource> provisionedResourceClass);

    <T extends ProvisionedDataDestinationResource> StatusChecker<T> resolve(T resource);
}
