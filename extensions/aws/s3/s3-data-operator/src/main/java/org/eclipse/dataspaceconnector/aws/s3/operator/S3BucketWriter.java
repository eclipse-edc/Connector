/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.aws.s3.operator;


import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.aws.s3.core.S3ClientProvider;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataWriter;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

public class S3BucketWriter implements DataWriter {

    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;
    private final TypeManager typeManager;
    private final S3ClientProvider clientProvider;

    public S3BucketWriter(Monitor monitor, TypeManager typeManager, RetryPolicy<Object> retryPolicy, S3ClientProvider clientProvider) {
        this.monitor = monitor;
        this.typeManager = typeManager;
        this.retryPolicy = retryPolicy;
        this.clientProvider = clientProvider;
    }

    @Override
    public boolean canHandle(String type) {
        return S3BucketSchema.TYPE.equals(type);
    }

    @Override
    public Result<Void> write(DataAddress destination, String name, InputStream data, String secretToken) {
        var bucketName = destination.getProperty(S3BucketSchema.BUCKET_NAME);
        var region = destination.getProperty(S3BucketSchema.REGION);
        var awsSecretToken = typeManager.readValue(secretToken, AwsTemporarySecretToken.class);

        try (var s3 = clientProvider.provide(region, awsSecretToken)) {
            var request = createRequest(bucketName, name);
            var completionMarker = createRequest(bucketName, name + ".complete");
            monitor.debug("Data request: begin transfer...");
            Failsafe.with(retryPolicy).get(() -> s3.putObject(request, RequestBody.fromBytes(data.readAllBytes())));
            Failsafe.with(retryPolicy).get(() -> s3.putObject(completionMarker, RequestBody.empty()));
            monitor.debug("Data request done.");
            return Result.success();
        } catch (S3Exception ex) {
            monitor.severe("Data request: transfer failed!", ex);
            return Result.failure("Data transfer failed");
        }
    }

    private static PutObjectRequest createRequest(String bucketName, String objectKey) {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
    }
}

