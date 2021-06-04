/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.spi.types.domain.transfer;

/**
 * Used to check different status of a resource, e.g. whether a particular ProvisionedResource is "complete", e.g. if a data transfer has completed.
 * Imagine a few files being copied into an FTP folder. Then the completion check might list the folders contents and
 * check whether a file with the "*.complete" extension exists.
 *
 * @param <T> the concrete type of ProvisionedResource
 */
@FunctionalInterface
public interface StatusChecker<T extends ProvisionedResource> {
    /**
     * checks whether a particular ProvisionedResource is "complete", i.e. whether the data transfer is finished.
     *
     * @param provisionedResource The provisioned resource in question.
     * @return True if complete, false otherwise
     */
    boolean isComplete(T provisionedResource);
}
