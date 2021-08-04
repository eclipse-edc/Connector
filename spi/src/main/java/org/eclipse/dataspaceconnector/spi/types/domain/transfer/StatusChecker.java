/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.types.domain.transfer;

/**
 * Used to check different status of a resource, e.g. whether a particular ProvisionedResource is "complete", e.g. if a data transfer has completed.
 * Imagine a few files being copied into an FTP folder. Then the completion check might list the folders contents and
 * check whether a file with the "*.complete" extension exists.
 *
 * @param <T> the concrete type of ProvisionedResource
 */
@FunctionalInterface
public interface StatusChecker<T extends ProvisionedDataDestinationResource> {
    /**
     * checks whether a particular ProvisionedResource is "complete", i.e. whether the data transfer is finished.
     *
     * @param provisionedResource The provisioned resource in question.
     * @return True if complete, false otherwise
     */
    boolean isComplete(T provisionedResource);
}
