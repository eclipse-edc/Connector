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

import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.Nullable;

import static java.util.UUID.randomUUID;

public class ObjectStorageDefinitionConsumerGenerator implements ResourceDefinitionGenerator {

    @Override
    public @Nullable ResourceDefinition generate(TransferProcess process, Policy policy) {
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
