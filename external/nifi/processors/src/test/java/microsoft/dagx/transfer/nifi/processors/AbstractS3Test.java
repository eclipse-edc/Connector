/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package microsoft.dagx.transfer.nifi.processors;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import static com.microsoft.dagx.spi.util.ConfigurationFunctions.propOrEnv;
import static org.junit.jupiter.api.Assertions.fail;

public class AbstractS3Test {

    protected final static String REGION = System.getProperty("it.aws.region", Regions.US_EAST_1.getName());
    // Adding REGION to bucket prevents errors of
    //      "A conflicting conditional operation is currently in progress against this resource."
    // when bucket is rapidly added/deleted and consistency propagation causes this error.
    // (Should not be necessary if REGION remains static, but added to prevent future frustration.)
    // [see http://stackoverflow.com/questions/13898057/aws-error-message-a-conflicting-conditional-operation-is-currently-in-progress]
    protected static AmazonS3 client;

    @BeforeAll
    public static void oneTimeSetup() {
        //         this is necessary because the @EnabledIf... annotation does not prevent @BeforeAll to be called
        var isCi = propOrEnv("CI", "false");
        if (!Boolean.parseBoolean(isCi)) {
            return;
        }
        final AWSCredentials credentials = getCredentials();
        client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(REGION).build();

    }

    protected void createBucket(AmazonS3 client, String bucketName, String region) {
        if (client.doesBucketExistV2(bucketName)) {
            fail("Bucket " + bucketName + " exists. Choose a different bucket name to continue test");
        }

        CreateBucketRequest request = region.contains("east")
                ? new CreateBucketRequest(bucketName) // See https://github.com/boto/boto3/issues/125
                : new CreateBucketRequest(bucketName, region);
        client.createBucket(request);

        if (!client.doesBucketExistV2(bucketName)) {
            fail("Setup incomplete, tests will fail");
        }
    }

    protected void deleteBucket(String bucketName, AmazonS3 client) {
        // Empty the bucket before deleting it.
        try {
            if (client == null) {
                return;
            }

            ObjectListing objectListing = client.listObjects(bucketName);

            while (true) {
                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    client.deleteObject(bucketName, objectSummary.getKey());
                }

                if (objectListing.isTruncated()) {
                    objectListing = client.listNextBatchOfObjects(objectListing);
                } else {
                    break;
                }
            }

            DeleteBucketRequest dbr = new DeleteBucketRequest(bucketName);
            client.deleteBucket(dbr);
        } catch (final AmazonS3Exception e) {
            System.err.println("Unable to delete bucket " + bucketName + e.toString());
        }

        if (client.doesBucketExistV2(bucketName)) {
            fail("Incomplete teardown, subsequent tests might fail");
        }

    }

    protected static @NotNull AWSCredentials getCredentials() {

        var accessKeyId = propOrEnv("S3_ACCESS_KEY_ID", null);
        var secretKey = propOrEnv("S3_SECRET_ACCESS_KEY", null);

        return new BasicAWSCredentials(accessKeyId, secretKey);
    }

    protected PutObjectResult putTestFile(String key, File file, String bucketName) throws AmazonS3Exception {
        PutObjectRequest putRequest = new PutObjectRequest(bucketName, key, file);

        return client.putObject(putRequest);
    }

    protected S3Object fetchTestFile(String bucket, String key) {
        GetObjectRequest request = new GetObjectRequest(bucket, key);
        return client.getObject(request);
    }



}
