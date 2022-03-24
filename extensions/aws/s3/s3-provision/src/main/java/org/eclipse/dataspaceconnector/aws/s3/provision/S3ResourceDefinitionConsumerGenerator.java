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

package org.eclipse.dataspaceconnector.aws.s3.provision;

import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import software.amazon.awssdk.regions.Region;

import static java.util.UUID.randomUUID;

/**
 * Generates S3 buckets on the consumer (requesting connector) that serve as data destinations.
 */
public class S3ResourceDefinitionConsumerGenerator implements ResourceDefinitionGenerator {

    @Override
    public ResourceDefinition generate(TransferProcess process, Policy policy) {
        var request = process.getDataRequest();
        if (request.getDestinationType() != null) {
            if (!S3BucketSchema.TYPE.equals(request.getDestinationType())) {
                return null;
            }
            // FIXME generate region from policy engine
            return S3BucketResourceDefinition.Builder.newInstance().id(randomUUID().toString()).bucketName(process.getId()).regionId(Region.US_EAST_1.id()).build();

        } else if (request.getDataDestination() == null || !(request.getDataDestination().getType().equals(S3BucketSchema.TYPE))) {
            return null;
        }
        DataAddress destination = request.getDataDestination();
        String id = randomUUID().toString();
        return S3BucketResourceDefinition.Builder.newInstance().id(id).bucketName(destination.getProperty(S3BucketSchema.BUCKET_NAME)).regionId(destination.getProperty(S3BucketSchema.REGION)).build();
    }
}
