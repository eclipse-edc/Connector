/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.dataspaceconnector.provision.azure.blob;

import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.schema.azure.*;

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
