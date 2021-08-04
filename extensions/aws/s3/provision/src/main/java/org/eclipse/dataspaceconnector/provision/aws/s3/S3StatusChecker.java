/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.provision.aws.s3;

import org.eclipse.dataspaceconnector.provision.aws.provider.ClientProvider;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusChecker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.util.concurrent.CompletionException;

public class S3StatusChecker implements StatusChecker<S3BucketProvisionedResource> {
    private final ClientProvider clientProvider;
    private final RetryPolicy<Object> retryPolicy;

    public S3StatusChecker(ClientProvider clientProvider, RetryPolicy<Object> retryPolicy) {
        this.clientProvider = clientProvider;
        this.retryPolicy = retryPolicy;

    }

    @Override
    public boolean isComplete(S3BucketProvisionedResource definition) {
        try {
            var bucketName = definition.getBucketName();
            var region = definition.getRegion();

            var s3client = clientProvider.clientFor(S3AsyncClient.class, region);

            var rq = ListObjectsRequest.builder().bucket(bucketName).build();
            var response = Failsafe.with(retryPolicy)
                    .getStageAsync(() -> s3client.listObjects(rq))
                    .join();
            return response.contents().stream().anyMatch(s3object -> s3object.key().endsWith(".complete"));
        } catch (CompletionException cpe) {
            if (cpe.getCause() instanceof NoSuchBucketException) {
                return false;
            }
            throw cpe;
        }
    }
}
