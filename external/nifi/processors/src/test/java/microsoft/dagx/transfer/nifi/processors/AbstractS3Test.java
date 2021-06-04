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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;

import static com.microsoft.dagx.common.ConfigurationFunctions.propOrEnv;
import static org.junit.jupiter.api.Assertions.fail;

public class AbstractS3Test {

    protected final static String REGION = System.getProperty("it.aws.region", Regions.US_EAST_1.getName());
    // Adding REGION to bucket prevents errors of
    //      "A conflicting conditional operation is currently in progress against this resource."
    // when bucket is rapidly added/deleted and consistency propagation causes this error.
    // (Should not be necessary if REGION remains static, but added to prevent future frustration.)
    // [see http://stackoverflow.com/questions/13898057/aws-error-message-a-conflicting-conditional-operation-is-currently-in-progress]
    protected AmazonS3 client;
    protected String bucketName;
    protected AWSCredentials credentials;

    @BeforeEach
    public void setupClient() {
        bucketName = "test-bucket-" + System.currentTimeMillis() + "-" + REGION;
        credentials = getCredentials();
        client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(REGION).build();
        createBucket(bucketName);
    }

    @AfterEach
    void cleanup() {
        deleteBucket(bucketName);
    }

    protected @NotNull AWSCredentials getCredentials() {
        var accessKeyId = propOrEnv("S3_ACCESS_KEY_ID", null);
        var secretKey = propOrEnv("S3_SECRET_ACCESS_KEY", null);

        return new BasicAWSCredentials(accessKeyId, secretKey);
    }

    protected void createBucket(String bucketName) {
        if (client.doesBucketExistV2(bucketName)) {
            fail("Bucket " + bucketName + " exists. Choose a different bucket name to continue test");
        }

        CreateBucketRequest request = AbstractS3Test.REGION.contains("east")
                ? new CreateBucketRequest(bucketName) // See https://github.com/boto/boto3/issues/125
                : new CreateBucketRequest(bucketName, AbstractS3Test.REGION);
        client.createBucket(request);

        if (!client.doesBucketExistV2(bucketName)) {
            fail("Setup incomplete, tests will fail");
        }
    }

    protected void deleteBucket(String bucketName) {
        // Empty the bucket before deleting it, otherwise the AWS S3 API fails
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
        } catch (AmazonS3Exception e) {
            System.err.println("Unable to delete bucket " + bucketName + e);
        }

        if (client.doesBucketExistV2(bucketName)) {
            fail("Incomplete teardown, subsequent tests might fail");
        }

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
