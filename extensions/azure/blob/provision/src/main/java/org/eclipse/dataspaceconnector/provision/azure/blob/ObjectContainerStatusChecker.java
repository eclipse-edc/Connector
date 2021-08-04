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

package org.eclipse.dataspaceconnector.provision.azure.blob;

import org.eclipse.dataspaceconnector.common.azure.BlobStoreApi;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusChecker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

public class ObjectContainerStatusChecker implements StatusChecker<ObjectContainerProvisionedResource> {
    private final BlobStoreApi blobStoreApi;
    private final RetryPolicy<Object> retryPolicy;

    public ObjectContainerStatusChecker(BlobStoreApi blobStoreApi, RetryPolicy<Object> retryPolicy) {
        this.blobStoreApi = blobStoreApi;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public boolean isComplete(ObjectContainerProvisionedResource provisionedResource) {
        if (!blobStoreApi.exists(provisionedResource.getAccountName(), provisionedResource.getContainerName())) {
            return false;
        }

        return Failsafe.with(retryPolicy).get(() -> blobStoreApi.listContainer(provisionedResource.getAccountName(), provisionedResource.getContainerName())
                .stream().anyMatch(bci -> bci.getName().endsWith(".complete")));
    }
}
