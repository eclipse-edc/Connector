/*
 *  Copyright (c) 2021 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Siemens AG - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.controller;

import org.eclipse.dataspaceconnector.core.schema.s3.S3BucketSchema;
import org.eclipse.dataspaceconnector.provision.aws.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.inline.spi.DataStreamPublisher;
import org.eclipse.dataspaceconnector.transfer.inline.spi.StreamContext;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class S3toS3DataStreamer implements DataStreamPublisher {

    private final Monitor monitor;
    private final Vault vault;
    private final TypeManager typeManager;
    private final DataAddressResolver dataAddressResolver;

    public S3toS3DataStreamer(Monitor monitor, Vault vault, TypeManager typeManager, DataAddressResolver dataAddressResolver) {
        this.monitor = monitor;
        this.vault = vault;
        this.typeManager = typeManager;
        this.dataAddressResolver = dataAddressResolver;
    }

    @Override
    public void initialize(StreamContext context) {

    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return "dataspaceconnector:s3".equals(dataRequest.getDestinationType());
    }

    @Override
    public Result<Void> notifyPublisher(DataRequest dataRequest) {
        var source = dataAddressResolver.resolveForAsset(dataRequest.getAssetId());

        String sourceKey = source.getKeyName();
        String sourceBucketName = source.getProperty(S3BucketSchema.BUCKET_NAME);

        var destinationKey = dataRequest.getDataDestination().getKeyName();
        var awsSecret = vault.resolveSecret(destinationKey);
        var destinationBucketName = dataRequest.getDataDestination().getProperty(S3BucketSchema.BUCKET_NAME);

        var region = dataRequest.getDataDestination().getProperty(S3BucketSchema.REGION);
        var dt = typeManager.readValue(awsSecret, AwsTemporarySecretToken.class);

        return copyToBucket(sourceBucketName, sourceKey, destinationBucketName, destinationKey, region, dt);
    }

    @NotNull
    private Result<Void> copyToBucket(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey, String region, AwsTemporarySecretToken dt) {
        try (S3Client s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsSessionCredentials.create(dt.getAccessKeyId(), dt.getSecretAccessKey(), dt.getSessionToken())))
                .region(Region.of(region))
                .build()) {

            try {
                monitor.debug("Data request: begin transfer...");

                CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                        .copySource(sourceBucketName + "/" + sourceKey)
                        .destinationBucket(destinationBucketName)
                        .destinationKey(destinationKey)
                        .build();

                var response = s3.copyObject(copyObjectRequest);
                String etag = response.copyObjectResult().eTag();
                monitor.debug("Data request done. eTag of new object is: " + etag);
            } catch (S3Exception tmpEx) {
                monitor.info("Data request: transfer not successful");
            }
            return Result.success();
        } catch (S3Exception | EdcException ex) {
            monitor.severe("Data request: transfer failed!");
            return Result.failure(ex.getLocalizedMessage());
        }
    }


}
