/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package microsoft.dagx.transfer.nifi.processors;

import com.amazonaws.auth.AWSCredentials;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
class FetchS3ObjectTest extends AbstractS3Test {

    private AWSCredentials credentials;

    @BeforeEach
    void setup() {
        credentials = getCredentials();
    }

    @Test
    @DisplayName("Verifies that a file is downloaded and the 'success' relation is traversed")
    public void testFetchFile_single() throws IOException {
        putTestFile("test-file", getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME));

        final TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, BUCKET_NAME);
        runner.setProperty(Properties.OBJECT_KEYS, "test-file");
        runner.setProperty(Properties.ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
        runner.setProperty(Properties.SECRET_ACCESS_KEY, credentials.getAWSSecretKey());


        final Map<String, String> attrs = new HashMap<>();
        runner.enqueue(new byte[0], attrs);

        runner.run(1);

        runner.assertAllFlowFilesTransferred(Properties.REL_SUCCESS, 1);
        final List<MockFlowFile> ffs = runner.getFlowFilesForRelationship(Properties.REL_SUCCESS);
        MockFlowFile ff = ffs.get(0);
        ff.assertContentEquals(getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME));
    }

    @Test
    @DisplayName("Verifies that the 'failure' relationship is traversed when a file is not found in S3")
    void fetchFile_single_whenNotExists() {
        final TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, BUCKET_NAME);
        runner.setProperty(Properties.OBJECT_KEYS, "non-existing-file");
        runner.setProperty(Properties.ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
        runner.setProperty(Properties.SECRET_ACCESS_KEY, credentials.getAWSSecretKey());


        final Map<String, String> attrs = new HashMap<>();
        runner.enqueue(new byte[0], attrs);

        runner.run(1);

        runner.assertAllFlowFilesTransferred(Properties.REL_FAILURE, 1);
        final List<MockFlowFile> ffs = runner.getFlowFilesForRelationship(Properties.REL_FAILURE);
        MockFlowFile ff = ffs.get(0);
    }

    @Test
    @DisplayName("Downloads a file and compares the contents")
    public void testFetchFile_single_assertContents() throws IOException {
        String key = "folder/1.txt";
        putTestFile(key, getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME));

        final TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, BUCKET_NAME);
        runner.setProperty(Properties.OBJECT_KEYS, key);
        runner.setProperty(Properties.ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
        runner.setProperty(Properties.SECRET_ACCESS_KEY, credentials.getAWSSecretKey());

        final Map<String, String> attrs = new HashMap<>();
        runner.enqueue(new byte[0], attrs);

        runner.run(1);

        runner.assertAllFlowFilesTransferred(Properties.REL_SUCCESS, 1);

        final List<MockFlowFile> ffs = runner.getFlowFilesForRelationship(Properties.REL_SUCCESS);
        final MockFlowFile out = ffs.iterator().next();

        final byte[] expectedBytes = Files.readAllBytes(getResourcePath(SAMPLE_FILE_RESOURCE_NAME));
        out.assertContentEquals(new String(expectedBytes));

        for (final Map.Entry<String, String> entry : out.getAttributes().entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    @Test
    @DisplayName("When a set of files is specified, the processor should fetch those")
    public void fetchFile_multiple_shouldEnumerate() throws IOException {
        File contents1 = getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME);
        putTestFile("hello1.txt", contents1);

        String testfile2 = "hello2.txt";
        File contents2 = getFileFromResourceName(testfile2);
        putTestFile(testfile2, contents2);

        final TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, BUCKET_NAME);
        runner.setProperty(Properties.OBJECT_KEYS, "[\"hello1.txt\",\"hello2.txt\"]");
        runner.setProperty(Properties.ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
        runner.setProperty(Properties.SECRET_ACCESS_KEY, credentials.getAWSSecretKey());


        final Map<String, String> attrs = new HashMap<>();
        runner.enqueue(new byte[0], attrs);

        runner.run(1);

        runner.assertAllFlowFilesTransferred(Properties.REL_SUCCESS, 2);
        final List<MockFlowFile> successfulFlowFiles = runner.getFlowFilesForRelationship(Properties.REL_SUCCESS);
        MockFlowFile ff = successfulFlowFiles.get(0);
        ff.assertContentEquals(getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME));
        MockFlowFile ff2 = successfulFlowFiles.get(1);
        ff2.assertContentEquals(contents2);
    }

    @Test
    @DisplayName("When a no list of files is specified, the processor should enumerate")
    public void fetchFile_none_shouldEnumerate() throws IOException {
        File contents1 = getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME);
        putTestFile("hello1.txt", contents1);

        String testfile2 = "hello2.txt";
        File contents2 = getFileFromResourceName(testfile2);
        putTestFile(testfile2, contents2);

        final TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, BUCKET_NAME);
        runner.setProperty(Properties.ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
        runner.setProperty(Properties.SECRET_ACCESS_KEY, credentials.getAWSSecretKey());


        final Map<String, String> attrs = new HashMap<>();
        runner.enqueue(new byte[0], attrs);

        runner.run(1);

        runner.assertAllFlowFilesTransferred(Properties.REL_SUCCESS, 2);
        final List<MockFlowFile> successfulFlowFiles = runner.getFlowFilesForRelationship(Properties.REL_SUCCESS);
        assertThat(successfulFlowFiles).hasSize(2);
        MockFlowFile ff = successfulFlowFiles.get(0);
        ff.assertContentEquals(contents1);
        MockFlowFile ff2 = successfulFlowFiles.get(1);
        ff2.assertContentEquals(contents2);
    }

}