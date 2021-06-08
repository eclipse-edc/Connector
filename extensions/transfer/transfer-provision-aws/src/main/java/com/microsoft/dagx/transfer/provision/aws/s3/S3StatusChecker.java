/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.provision.aws.s3;

import com.microsoft.dagx.spi.types.domain.transfer.StatusChecker;
import com.microsoft.dagx.transfer.provision.aws.provider.ClientProvider;
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
