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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.microsoft.dagx.spi.util.ConfigurationFunctions.propOrEnv;
import static org.junit.jupiter.api.Assertions.fail;

public class AbstractS3Test {
    protected final static String SAMPLE_FILE_RESOURCE_NAME = "hello.txt";
    protected final static String REGION = System.getProperty("it.aws.region", Regions.US_EAST_1.getName());
    // Adding REGION to bucket prevents errors of
    //      "A conflicting conditional operation is currently in progress against this resource."
    // when bucket is rapidly added/deleted and consistency propagation causes this error.
    // (Should not be necessary if REGION remains static, but added to prevent future frustration.)
    // [see http://stackoverflow.com/questions/13898057/aws-error-message-a-conflicting-conditional-operation-is-currently-in-progress]
    protected final static String BUCKET_NAME = "test-bucket-" + System.currentTimeMillis() + "-" + REGION;
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

        if (client.doesBucketExistV2(BUCKET_NAME)) {
            fail("Bucket " + BUCKET_NAME + " exists. Choose a different bucket name to continue test");
        }

        CreateBucketRequest request = REGION.contains("east")
                ? new CreateBucketRequest(BUCKET_NAME) // See https://github.com/boto/boto3/issues/125
                : new CreateBucketRequest(BUCKET_NAME, REGION);
        client.createBucket(request);

        if (!client.doesBucketExistV2(BUCKET_NAME)) {
            fail("Setup incomplete, tests will fail");
        }
    }

    @AfterAll
    public static void oneTimeTearDown() {
        // Empty the bucket before deleting it.
        try {
            if (client == null) {
                return;
            }

            ObjectListing objectListing = client.listObjects(BUCKET_NAME);

            while (true) {
                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    client.deleteObject(BUCKET_NAME, objectSummary.getKey());
                }

                if (objectListing.isTruncated()) {
                    objectListing = client.listNextBatchOfObjects(objectListing);
                } else {
                    break;
                }
            }

            DeleteBucketRequest dbr = new DeleteBucketRequest(BUCKET_NAME);
            client.deleteBucket(dbr);
        } catch (final AmazonS3Exception e) {
            System.err.println("Unable to delete bucket " + BUCKET_NAME + e.toString());
        }

        if (client.doesBucketExistV2(BUCKET_NAME)) {
            fail("Incomplete teardown, subsequent tests might fail");
        }

    }

    protected static @NotNull AWSCredentials getCredentials() {

        var accessKeyId = propOrEnv("S3_ACCESS_KEY_ID", null);
        var secretKey = propOrEnv("S3_SECRET_ACCESS_KEY", null);

        return new BasicAWSCredentials(accessKeyId, secretKey);
    }

    protected PutObjectResult putTestFile(String key, File file) throws AmazonS3Exception {
        PutObjectRequest putRequest = new PutObjectRequest(BUCKET_NAME, key, file);

        return client.putObject(putRequest);
    }

    protected S3Object fetchTestFile(String bucket, String key) {
        GetObjectRequest request = new GetObjectRequest(bucket, key);
        return client.getObject(request);
    }

    protected void putTestFileEncrypted(String key, File file) throws AmazonS3Exception, FileNotFoundException {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
        PutObjectRequest putRequest = new PutObjectRequest(BUCKET_NAME, key, new FileInputStream(file), objectMetadata);

        client.putObject(putRequest);
    }

    protected void putFileWithUserMetadata(String key, File file, Map<String, String> userMetadata) throws AmazonS3Exception, FileNotFoundException {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setUserMetadata(userMetadata);
        PutObjectRequest putRequest = new PutObjectRequest(BUCKET_NAME, key, new FileInputStream(file), objectMetadata);

        client.putObject(putRequest);
    }

    protected void putFileWithObjectTag(String key, File file, List<Tag> objectTags) {
        PutObjectRequest putRequest = new PutObjectRequest(BUCKET_NAME, key, file);
        putRequest.setTagging(new ObjectTagging(objectTags));
        PutObjectResult result = client.putObject(putRequest);
    }

    protected Path getResourcePath(String resourceName) {
        Path path = null;

        try {
            path = Paths.get(Thread.currentThread().getContextClassLoader().getResource(resourceName).toURI());
        } catch (URISyntaxException e) {
            fail("Resource: " + resourceName + " does not exist" + e.getLocalizedMessage());
        }

        return path;
    }

    protected File getFileFromResourceName(String resourceName) {
        URI uri = null;
        try {
            uri = Thread.currentThread().getContextClassLoader().getResource(resourceName).toURI();
        } catch (URISyntaxException e) {
            fail("Cannot proceed without File : " + resourceName);
        }

        return new File(uri);
    }
}
