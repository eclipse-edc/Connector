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

package org.eclipse.edc.connector.provision.azure.blob;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.transfer.spi.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.jetbrains.annotations.Nullable;

import static java.util.UUID.randomUUID;

public class ObjectStorageConsumerResourceDefinitionGenerator implements ConsumerResourceDefinitionGenerator {

    @Override
    public @Nullable ResourceDefinition generate(DataRequest dataRequest, Policy policy) {

        var destination = dataRequest.getDataDestination();
        var id = randomUUID().toString();
        var account = destination.getProperty(AzureBlobStoreSchema.ACCOUNT_NAME);
        var container = destination.getProperty(AzureBlobStoreSchema.CONTAINER_NAME);
        if (container == null) {
            container = randomUUID().toString();
        }
        return ObjectStorageResourceDefinition.Builder.newInstance().id(id).accountName(account).containerName(container).build();
    }

    @Override
    public boolean canGenerate(DataRequest dataRequest, Policy policy) {
        return AzureBlobStoreSchema.TYPE.equals(dataRequest.getDestinationType());
    }
}
