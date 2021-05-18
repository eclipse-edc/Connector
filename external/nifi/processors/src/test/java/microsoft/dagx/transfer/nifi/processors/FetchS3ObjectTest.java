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

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
class FetchS3ObjectTest extends AbstractS3Test {

    private AWSCredentials credentials;

    @BeforeEach
    void setup() {
        credentials = getCredentials();
    }

    @Test
    @DisplayName("Verifies that a file is downloaded and the 'success' relation is traversed")
    public void testFetchFile() throws IOException {
        putTestFile("test-file", getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME));

        final TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, BUCKET_NAME);
        runner.setProperty(Properties.OBJECT_KEY, "test-file");
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
    void testFetchFIle_whenNotExists() {
        final TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, BUCKET_NAME);
        runner.setProperty(Properties.OBJECT_KEY, "non-existing-file");
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
    public void testFetchFile_assertContents() throws IOException {
        String key = "folder/1.txt";
        putTestFile(key, getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME));

        final TestRunner runner = TestRunners.newTestRunner(new FetchS3Object());

        runner.setProperty(Properties.REGION, REGION);
        runner.setProperty(Properties.BUCKET, BUCKET_NAME);

        runner.setProperty(Properties.OBJECT_KEY, key);
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

}