/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.provision.azure;

import com.microsoft.dagx.schema.azure.AzureBlobStoreSchema;
import com.microsoft.dagx.spi.transfer.provision.ResourceDefinitionGenerator;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.Nullable;

import static java.util.UUID.randomUUID;

public class ObjectStorageDefinitionClientGenerator implements ResourceDefinitionGenerator {
    @Override
    public @Nullable ResourceDefinition generate(TransferProcess process) {
        var request = process.getDataRequest();
        if (request.getDataDestination() == null || request.getDestinationType() == null || !AzureBlobStoreSchema.TYPE.equals(request.getDestinationType())) {
            return null;
        }

        DataAddress destination = request.getDataDestination();
        String id = randomUUID().toString();
        var account = destination.getProperty(AzureBlobStoreSchema.ACCOUNT_NAME);
        var container = destination.getProperty(AzureBlobStoreSchema.CONTAINER_NAME);
        if (container == null) {
            container = randomUUID().toString();
        }
        return ObjectStorageResourceDefinition.Builder.newInstance().id(id).accountName(account).containerName(container).build();
    }
}
