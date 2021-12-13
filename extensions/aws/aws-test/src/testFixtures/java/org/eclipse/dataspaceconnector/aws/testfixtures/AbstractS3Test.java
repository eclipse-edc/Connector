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

package org.eclipse.dataspaceconnector.aws.testfixtures;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Base class for tests that need an S3 bucket created and deleted on every test run.
 */
public abstract class AbstractS3Test {

    protected static final String REGION = System.getProperty("it.aws.region", Region.US_EAST_1.id());
    // Adding REGION to bucket prevents errors of
    //      "A conflicting conditional operation is currently in progress against this resource."
    // when bucket is rapidly added/deleted and consistency propagation causes this error.
    // (Should not be necessary if REGION remains static, but added to prevent future frustration.)
    // [see http://stackoverflow.com/questions/13898057/aws-error-message-a-conflicting-conditional-operation-is-currently-in-progress]

    protected final UUID processId = UUID.randomUUID();
    protected String bucketName = createBucketName();
    protected S3AsyncClient client;
    protected final String s3Endpoint = "http://localhost:9000";

    @BeforeEach
    public void setupClient() throws URISyntaxException {
        client = S3AsyncClient.builder()
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .region(Region.of(REGION))
                .credentialsProvider(this::getCredentials)
                .endpointOverride(new URI(s3Endpoint))
                .build();

        createBucket(bucketName);
    }

    @AfterEach
    void cleanup() {
        deleteBucket(bucketName);
    }

    @NotNull
    protected String createBucketName() {
        return "test-bucket-" + processId + "-" + REGION;
    }

    private @NotNull AwsCredentials getCredentials() {
        String profile = propOrEnv("AWS_PROFILE", null);
        if (profile != null) {
            return ProfileCredentialsProvider.create(profile).resolveCredentials();
        }

        var accessKeyId = propOrEnv("S3_ACCESS_KEY_ID", null);
        Objects.requireNonNull(accessKeyId, "S3_ACCESS_KEY_ID cannot be null!");
        var secretKey = propOrEnv("S3_SECRET_ACCESS_KEY", null);
        Objects.requireNonNull(secretKey, "S3_SECRET_ACCESS_KEY cannot be null");

        return AwsBasicCredentials.create(accessKeyId, secretKey);
    }

    protected void createBucket(String bucketName) {
        if (bucketExists(bucketName)) {
            fail("Bucket " + bucketName + " exists. Choose a different bucket name to continue test");
        }

        client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build()).join();

        if (!bucketExists(bucketName)) {
            fail("Setup incomplete, tests will fail");
        }
    }

    protected void deleteBucket(String bucketName) {
        try {
            if (client == null) {
                return;
            }

            // Empty the bucket before deleting it, otherwise the AWS S3 API fails
            deleteBucketObjects(bucketName);

            client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build()).join();
        } catch (Exception e) {
            System.err.println("Unable to delete bucket " + bucketName + e);
        }

        if (bucketExists(bucketName)) {
            fail("Incomplete teardown, subsequent tests might fail");
        }
    }

    private void deleteBucketObjects(String bucketName) {
        var objectListing = client.listObjects(ListObjectsRequest.builder().bucket(bucketName).build()).join();

        CompletableFuture.allOf(objectListing.contents().stream()
                .map(object -> client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(object.key()).build()))
                .toArray(CompletableFuture[]::new)).join();

        for (var objectSummary : objectListing.contents()) {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectSummary.key()).build()).join();
        }

        if (objectListing.isTruncated()) {
            deleteBucketObjects(bucketName);
        }
    }

    private boolean bucketExists(String bucketName) {
        try {
            HeadBucketRequest request = HeadBucketRequest.builder().bucket(bucketName).build();
            return client.headBucket(request).join()
                    .sdkHttpResponse()
                    .isSuccessful();
        } catch (CompletionException e) {
            if (e.getCause() instanceof NoSuchBucketException) {
                return false;
            } else {
                throw e;
            }
        }
    }

    protected CompletableFuture<PutObjectResponse> putTestFile(String key, File file, String bucketName) {
        return client.putObject(PutObjectRequest.builder().bucket(bucketName).key(key).build(), file.toPath());
    }

}
