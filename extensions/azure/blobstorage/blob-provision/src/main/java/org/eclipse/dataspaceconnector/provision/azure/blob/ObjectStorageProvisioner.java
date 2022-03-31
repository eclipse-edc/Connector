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

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureSasToken;
import org.eclipse.dataspaceconnector.azure.blob.core.api.BlobStoreApi;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.provision.DeprovisionResult;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionResult;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static net.jodah.failsafe.Failsafe.with;

public class ObjectStorageProvisioner implements Provisioner<ObjectStorageResourceDefinition, ObjectContainerProvisionedResource> {
    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;
    private final BlobStoreApi blobStoreApi;

    public ObjectStorageProvisioner(RetryPolicy<Object> retryPolicy, Monitor monitor, BlobStoreApi blobStoreApi) {
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
        this.blobStoreApi = blobStoreApi;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof ObjectStorageResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof ObjectContainerProvisionedResource;
    }

    @Override
    public CompletableFuture<ProvisionResult> provision(ObjectStorageResourceDefinition resourceDefinition, Policy policy) {
        String containerName = resourceDefinition.getContainerName();
        String accountName = resourceDefinition.getAccountName();

        monitor.info("Azure Storage Container request submitted: " + containerName);

        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1);

        return with(retryPolicy).getAsync(() -> blobStoreApi.exists(accountName, containerName))
                .thenCompose(exists -> {
                    if (exists) {
                        return reusingExistingContainer(containerName);
                    } else {
                        return createContainer(containerName, accountName);
                    }
                })
                .thenCompose(empty -> createContainerSasToken(containerName, accountName, expiryTime))
                .thenApply(writeOnlySas -> {
                    var resource = ObjectContainerProvisionedResource.Builder.newInstance()
                            .id(containerName)
                            .accountName(accountName)
                            .containerName(containerName)
                            .resourceDefinitionId(resourceDefinition.getId())
                            .transferProcessId(resourceDefinition.getTransferProcessId())
                            .resourceName("resource")
                            .hasToken(true)
                            .build();

                    var secretToken = new AzureSasToken("?" + writeOnlySas, expiryTime.toInstant().toEpochMilli());

                    var response = ProvisionResponse.Builder.newInstance().resource(resource).secretToken(secretToken).build();
                    return ProvisionResult.success(response);
                });
    }

    @Override
    public CompletableFuture<DeprovisionResult> deprovision(ObjectContainerProvisionedResource provisionedResource, Policy policy) {
        return with(retryPolicy).runAsync(() -> blobStoreApi.deleteContainer(provisionedResource.getAccountName(), provisionedResource.getContainerName()))
                //the sas token will expire automatically. there is no way of revoking them other than a stored access policy
                .thenApply(empty -> DeprovisionResult.success(DeprovisionedResource.Builder.newInstance().provisionedResourceId(provisionedResource.getId()).build()));
    }

    @NotNull
    private CompletableFuture<Void> reusingExistingContainer(String containerName) {
        monitor.debug("ObjectStorageProvisioner: re-use existing container " + containerName);
        return CompletableFuture.completedFuture(null);
    }

    @NotNull
    private CompletableFuture<Void> createContainer(String containerName, String accountName) {
        return with(retryPolicy)
                .runAsync(() -> {
                    blobStoreApi.createContainer(accountName, containerName);
                    monitor.debug("ObjectStorageProvisioner: created a new container " + containerName);
                });
    }

    @NotNull
    private CompletableFuture<String> createContainerSasToken(String containerName, String accountName, OffsetDateTime expiryTime) {
        return with(retryPolicy)
                .getAsync(() -> {
                    monitor.debug("ObjectStorageProvisioner: obtained temporary SAS token (write-only)");
                    return blobStoreApi.createContainerSasToken(accountName, containerName, "w", expiryTime);
                });
    }
}
