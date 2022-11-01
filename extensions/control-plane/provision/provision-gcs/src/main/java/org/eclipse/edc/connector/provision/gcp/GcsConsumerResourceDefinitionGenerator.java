/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

package org.eclipse.edc.connector.provision.gcp;

import org.eclipse.edc.connector.transfer.spi.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.gcp.storage.GcsStoreSchema;
import org.eclipse.edc.policy.model.Policy;
import org.jetbrains.annotations.Nullable;

import static java.util.UUID.randomUUID;

public class GcsConsumerResourceDefinitionGenerator implements ConsumerResourceDefinitionGenerator {
    @Override
    public @Nullable
    ResourceDefinition generate(DataRequest dataRequest, Policy policy) {
        if (dataRequest.getDataDestination() == null || dataRequest.getDestinationType() == null || !GcsStoreSchema.TYPE.equals(dataRequest.getDestinationType())) {
            return null;
        }

        var destination = dataRequest.getDataDestination();
        var id = randomUUID().toString();
        var location = destination.getProperty(GcsStoreSchema.LOCATION);
        var storageClass = destination.getProperty(GcsStoreSchema.STORAGE_CLASS);

        return GcsResourceDefinition.Builder.newInstance().id(id).location(location)
                .storageClass(storageClass).build();
    }
}
