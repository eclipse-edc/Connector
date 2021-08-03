/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.edc.provision.azure.blob;

import org.eclipse.edc.common.azure.BlobStoreApi;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.transfer.provision.ProvisionContext;
import org.eclipse.edc.spi.transfer.provision.Provisioner;
import org.eclipse.edc.spi.transfer.response.ResponseStatus;
import org.eclipse.edc.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.edc.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.edc.provision.azure.AzureSasToken;
import net.jodah.failsafe.RetryPolicy;

import java.time.OffsetDateTime;

import static net.jodah.failsafe.Failsafe.with;

/**
 *
 */
public class ObjectStorageProvisioner implements Provisioner<ObjectStorageResourceDefinition, ObjectContainerProvisionedResource> {
    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;
    private final BlobStoreApi blobStoreApi;
    private ProvisionContext context;

    public ObjectStorageProvisioner(RetryPolicy<Object> retryPolicy, Monitor monitor, BlobStoreApi blobStoreApi) {
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
        this.blobStoreApi = blobStoreApi;
    }

    @Override
    public void initialize(ProvisionContext context) {
        this.context = context;
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
    public ResponseStatus provision(ObjectStorageResourceDefinition resourceDefinition) {
        String containerName = resourceDefinition.getContainerName();
        String accountName = resourceDefinition.getAccountName();

        monitor.info("Azure Storage Container request submitted: " + containerName);

        //create the container
        if (!with(retryPolicy).get(() -> blobStoreApi.exists(accountName, containerName))) {
            with(retryPolicy).run(() -> blobStoreApi.createContainer(accountName, containerName));
            monitor.debug("ObjectStorageProvisioner: created a new container " + containerName);
        } else {
            monitor.debug("ObjectStorageProvisioner: re-use existing container " + containerName);
        }

        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1);

        // the "?" is actually important, otherwise downstream transfer tools like nifi might complain

        String writeOnlySas = "?" + with(retryPolicy).get(() -> blobStoreApi.createContainerSasToken(accountName, containerName, "w", expiryTime));
        monitor.debug("ObjectStorageProvisioner: obtained temporary SAS token (write-only)");

        var resource = ObjectContainerProvisionedResource.Builder.newInstance()
                .id(containerName)
                .accountName(accountName)
                .containerName(containerName)
                .resourceDefinitionId(resourceDefinition.getId())
                .transferProcessId(resourceDefinition.getTransferProcessId()).build();

        var secretToken = new AzureSasToken(writeOnlySas, expiryTime.toInstant().toEpochMilli());

        context.callback(resource, secretToken);

        return ResponseStatus.OK;
    }

    @Override
    public ResponseStatus deprovision(ObjectContainerProvisionedResource provisionedResource) {
        Throwable throwable = null;
        try {
            with(retryPolicy).run(() -> blobStoreApi.deleteContainer(provisionedResource.getAccountName(), provisionedResource.getContainerName()));
        } catch (Exception ex) {
            throwable = ex;
        }
        //the sas token will expire automatically. there is no way of revoking them other than a stored access policy
        context.deprovisioned(provisionedResource, throwable);
        return ResponseStatus.OK;
    }
}
