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

package org.eclipse.dataspaceconnector.provision.azure.blob;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.common.azure.BlobStoreApi;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusChecker;

import java.util.List;

import static java.lang.String.format;

public class ObjectContainerStatusChecker implements StatusChecker {
    private final BlobStoreApi blobStoreApi;
    private final RetryPolicy<Object> retryPolicy;

    public ObjectContainerStatusChecker(BlobStoreApi blobStoreApi, RetryPolicy<Object> retryPolicy) {
        this.blobStoreApi = blobStoreApi;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public boolean isComplete(String id, List<ProvisionedResource> resources) {
        for (var resource : resources) {
            if (resource instanceof ObjectContainerProvisionedResource) {
                var provisionedResource = (ObjectContainerProvisionedResource) resource;
                if (!blobStoreApi.exists(provisionedResource.getAccountName(), provisionedResource.getContainerName())) {
                    return false;
                }

                return Failsafe.with(retryPolicy).get(() -> blobStoreApi.listContainer(provisionedResource.getAccountName(), provisionedResource.getContainerName())
                        .stream().anyMatch(bci -> bci.getName().endsWith(".complete")));

            }
        }
        throw new EdcException(format("No object container resource was associated with the transfer process: %s - cannot determine completion.", id));
    }

}
