/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package microsoft.dagx.transfer.nifi.processors;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.File;
import java.nio.file.Files;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static microsoft.dagx.transfer.nifi.processors.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
class FetchS3ObjectTest extends AbstractS3Test {

    private RetryPolicy<Object> retryPolicy;

    @BeforeEach
    void setup() {
        retryPolicy = new RetryPolicy<>().withBackoff(500, 5000, ChronoUnit.MILLIS).withMaxRetries(5).handle(AssertionError.class);
    }

    @Test
    @DisplayName("Verifies that a file is downloaded and the 'success' relation is traversed")
    public void testFetchFile_single() {
        putTestFile("test-file", getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME), bucketName);

        TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, bucketName);
        runner.setProperty(Properties.OBJECT_KEYS, "test-file");
        runner.setProperty(Properties.ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
        runner.setProperty(Properties.SECRET_ACCESS_KEY, credentials.getAWSSecretKey());


        Map<String, String> attrs = new HashMap<>();
        runner.enqueue(new byte[0], attrs);

        runner.run(1);

        Failsafe.with(retryPolicy).run(() -> {
            System.out.println("");
            List<MockFlowFile> ffs = runner.getFlowFilesForRelationship(Properties.REL_SUCCESS);
            assertThat(ffs).hasSize(1);

            MockFlowFile ff = ffs.get(0);
            ff.assertContentEquals(getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME));
            assertThat(ffs).allSatisfy(mff -> assertThat(mff.getAttribute("filename")).contains("test-file"));
        });

    }

    @Test
    @DisplayName("Verifies that the 'failure' relationship is traversed when a file is not found in S3")
    void fetchFile_single_whenNotExists() {
        TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, bucketName);
        runner.setProperty(Properties.OBJECT_KEYS, "non-existing-file");
        runner.setProperty(Properties.ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
        runner.setProperty(Properties.SECRET_ACCESS_KEY, credentials.getAWSSecretKey());


        Map<String, String> attrs = new HashMap<>();
        runner.enqueue(new byte[0], attrs);

        runner.run(1);
        Failsafe.with(retryPolicy).run(() -> {
            List<MockFlowFile> ffs = runner.getFlowFilesForRelationship(Properties.REL_FAILURE);
            assertThat(ffs).hasSize(1);
            MockFlowFile ff = ffs.get(0);
        });
    }

    @Test
    @DisplayName("Downloads a file and compares the contents")
    public void testFetchFile_single_assertContents() {
        String key = "folder/1.txt";
        putTestFile(key, getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME), bucketName);

        TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, bucketName);
        runner.setProperty(Properties.OBJECT_KEYS, key);
        runner.setProperty(Properties.ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
        runner.setProperty(Properties.SECRET_ACCESS_KEY, credentials.getAWSSecretKey());

        Map<String, String> attrs = new HashMap<>();
        runner.enqueue(new byte[0], attrs);

        runner.run(1);


        Failsafe.with(retryPolicy).run(() -> {
            List<MockFlowFile> ffs = runner.getFlowFilesForRelationship(Properties.REL_SUCCESS);
            assertThat(ffs).hasSize(1);
            assertThat(ffs).allSatisfy(mff -> assertThat(mff.getAttribute("filename")).contains("folder/1.txt"));
            MockFlowFile out = ffs.iterator().next();

            byte[] expectedBytes = Files.readAllBytes(getResourcePath(SAMPLE_FILE_RESOURCE_NAME));
            out.assertContentEquals(new String(expectedBytes));


            for (Map.Entry<String, String> entry : out.getAttributes().entrySet()) {
                System.out.println(entry.getKey() + " : " + entry.getValue());
            }
        });
    }

    @Test
    @DisplayName("When a set of files is specified, the processor should fetch those")
    public void fetchFile_multiple_shouldEnumerate() {
        File contents1 = getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME);
        putTestFile("hello1.txt", contents1, bucketName);

        String testfile2 = "hello2.txt";
        File contents2 = getFileFromResourceName(testfile2);
        putTestFile(testfile2, contents2, bucketName);

        TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, bucketName);
        runner.setProperty(Properties.OBJECT_KEYS, "[\"hello1.txt\",\"hello2.txt\"]");
        runner.setProperty(Properties.ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
        runner.setProperty(Properties.SECRET_ACCESS_KEY, credentials.getAWSSecretKey());


        Map<String, String> attrs = new HashMap<>();
        runner.enqueue(new byte[0], attrs);

        runner.run(1);

        Failsafe.with(retryPolicy).run(() -> {
            List<MockFlowFile> successfulFlowFiles = runner.getFlowFilesForRelationship(Properties.REL_SUCCESS);
            assertThat(successfulFlowFiles).hasSize(2);
            assertThat(successfulFlowFiles).allSatisfy(mff -> assertThat(mff.getAttribute("filename")).contains("hello"));

            MockFlowFile ff = successfulFlowFiles.get(0);
            ff.assertContentEquals(getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME));
            MockFlowFile ff2 = successfulFlowFiles.get(1);
            ff2.assertContentEquals(contents2);
        });
    }

    @Test
    @DisplayName("When a set of files is specified, the processor should fetch those")
    public void fetchFile_multiple_someDontExist_shouldEnumerate() {
        File contents1 = getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME);
        putTestFile("hello1.txt", contents1, bucketName);


        TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, bucketName);
        runner.setProperty(Properties.OBJECT_KEYS, "[\"hello1.txt\",\"notexist.txt\"]");
        runner.setProperty(Properties.ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
        runner.setProperty(Properties.SECRET_ACCESS_KEY, credentials.getAWSSecretKey());


        Map<String, String> attrs = new HashMap<>();
        runner.enqueue(new byte[0], attrs);

        runner.run(1);

        Failsafe.with(retryPolicy).run(() -> {
            List<MockFlowFile> successfulFlowFiles = runner.getFlowFilesForRelationship(Properties.REL_SUCCESS);
            assertThat(successfulFlowFiles).hasSize(1);

            assertThat(successfulFlowFiles).allSatisfy(mff -> assertThat(mff.getAttribute("filename")).contains("hello1"));

            List<MockFlowFile> failedFlowFiles = runner.getFlowFilesForRelationship(Properties.REL_FAILURE);
            assertThat(failedFlowFiles).hasSize(1);
        });

    }

    @Test
    @DisplayName("When no list of files is specified, the processor should enumerate all")
    public void fetchFile_none_shouldEnumerate() {
        File contents1 = getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME);
        putTestFile("file1.txt", contents1, bucketName);

        String testfile2 = "hello2.txt";
        File contents2 = getFileFromResourceName(testfile2);
        putTestFile("file2.txt", contents2, bucketName);

        TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, bucketName);
        runner.setProperty(Properties.ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
        runner.setProperty(Properties.SECRET_ACCESS_KEY, credentials.getAWSSecretKey());


        Map<String, String> attrs = new HashMap<>();
        runner.enqueue(new byte[0], attrs);

        runner.run(1);

        Failsafe.with(retryPolicy).run(() -> {
            runner.assertAllFlowFilesTransferred(Properties.REL_SUCCESS);
            List<MockFlowFile> successfulFlowFiles = runner.getFlowFilesForRelationship(Properties.REL_SUCCESS);

            assertThat(successfulFlowFiles).allSatisfy(mff -> assertThat(mff.getAttribute("filename")).isNotEmpty());

            assertThat(successfulFlowFiles).hasSizeGreaterThanOrEqualTo(2);
        });
    }
}