/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.provision.azure;

import com.microsoft.dagx.spi.transfer.provision.ProvisionContext;
import com.microsoft.dagx.spi.transfer.provision.Provisioner;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.domain.transfer.DestinationSecretToken;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

/**
 *
 */
public class ObjectStorageProvisioner implements Provisioner<ObjectStorageResourceDefinition, ObjectContainerProvisionedResource> {
    private ProvisionContext context;

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
        var secretToken = new DestinationSecretToken("", "", "", -1);
        var resource = new ObjectContainerProvisionedResource();
        context.callback(resource, secretToken);
        return ResponseStatus.OK;
    }

    @Override
    public ResponseStatus deprovision(ObjectContainerProvisionedResource provisionedResource) {
        return ResponseStatus.OK;
    }
}
