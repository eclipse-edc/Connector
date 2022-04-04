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
import org.eclipse.dataspaceconnector.spi.transfer.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.jetbrains.annotations.Nullable;

import static java.util.UUID.randomUUID;

public class ObjectStorageConsumerResourceDefinitionGenerator implements ConsumerResourceDefinitionGenerator {

    @Override
    public @Nullable ResourceDefinition generate(DataRequest dataRequest, Policy policy) {
        if (dataRequest.getDataDestination() == null || dataRequest.getDestinationType() == null || !AzureBlobStoreSchema.TYPE.equals(dataRequest.getDestinationType())) {
            return null;
        }

        var destination = dataRequest.getDataDestination();
        var id = randomUUID().toString();
        var account = destination.getProperty(AzureBlobStoreSchema.ACCOUNT_NAME);
        var container = destination.getProperty(AzureBlobStoreSchema.CONTAINER_NAME);
        if (container == null) {
            container = randomUUID().toString();
        }
        return ObjectStorageResourceDefinition.Builder.newInstance().id(id).accountName(account).containerName(container).build();
    }
}
