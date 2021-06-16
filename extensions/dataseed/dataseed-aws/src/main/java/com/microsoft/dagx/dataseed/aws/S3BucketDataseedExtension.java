/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.dataseed.aws;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.transfer.provision.aws.provider.ClientProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class S3BucketDataseedExtension implements ServiceExtension {

    private static final String REGION = Region.US_EAST_1.toString();
    private static final String BUCKET_NAME = "dagx-src-bucket";
    private static final String TEST_FILE_NAME = "donald.png";
    private static final String TEST_FILE_NAME2 = "dagobert.png";
    private Monitor monitor;
    private S3AsyncClient s3Client;

    @Override
    public void initialize(ServiceExtensionContext context) {

        var clientProvider = context.getService(ClientProvider.class);
        monitor = context.getMonitor();
        s3Client = clientProvider.clientFor(S3AsyncClient.class, REGION);


        monitor.info("AWS DataSeed: uploading a few test files");
        var is = getClass().getClassLoader().getResourceAsStream(TEST_FILE_NAME);
        var is2 = getClass().getClassLoader().getResourceAsStream(TEST_FILE_NAME2);
        if (is == null) {
            monitor.severe("AWS DataSeed: cannot find seed file " + TEST_FILE_NAME);
            return;
        }
        if (is2 == null) {
            monitor.severe("AWS DataSeed: cannot find seed file " + TEST_FILE_NAME2);
            return;
        }
        byte[] bytes;
        byte[] bytes2;
        try {
            bytes = is.readAllBytes();
            bytes2 = is2.readAllBytes();
        } catch (IOException e) {
            monitor.severe("AWS DataSeed: cannot read seed files");
            return;
        }

        s3Client.listBuckets()
                .thenCompose(listBucketsResponse -> {

                    if (listBucketsResponse.buckets().stream().noneMatch(b -> b.name().equals(BUCKET_NAME))) {
                        // need to call "allOf" here to achieve type constraint compatibility
                        return s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }

                })
                .thenCompose(createBucketResponse -> {
                    if (createBucketResponse == null) {
                        monitor.debug("AWS DataSeed: bucket already exists, will reuse");
                    }
                    monitor.info("AWS DataSeed: uploading test file '" + TEST_FILE_NAME + "'");
                    final CompletableFuture<PutObjectResponse> putResponse1 = s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key("testimage.jpg").build(), AsyncRequestBody.fromBytes(bytes));
                    monitor.info("AWS DataSeed: uploading test file '" + TEST_FILE_NAME2 + "'");
                    final CompletableFuture<PutObjectResponse> putResponse2 = s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key("anotherimage.jpg").build(), AsyncRequestBody.fromBytes(bytes2));

                    return CompletableFuture.allOf(putResponse1, putResponse2);
                })
                .whenComplete((o, throwable) -> {
                    if (throwable != null) {
                        monitor.severe("AWS DataSeed: An error happened during seeding", throwable);
                    } else {
                        monitor.info("AWS DataSeed: completed seeding");
                    }
                });
    }

    @Override
    public Set<String> requires() {
        return Set.of("dagx:clientprovider");
    }
}
