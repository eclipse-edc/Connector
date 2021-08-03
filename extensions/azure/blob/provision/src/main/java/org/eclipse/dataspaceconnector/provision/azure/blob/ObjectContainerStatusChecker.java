/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
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
