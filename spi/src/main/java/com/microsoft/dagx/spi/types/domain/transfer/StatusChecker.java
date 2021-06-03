/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.spi.types.domain.transfer;

@FunctionalInterface
public interface StatusChecker<T extends ProvisionedResource> {
    boolean isComplete(T provisionedResource);
}
