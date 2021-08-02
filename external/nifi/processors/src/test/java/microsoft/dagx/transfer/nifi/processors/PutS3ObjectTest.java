/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package microsoft.dagx.transfer.nifi.processors;

import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.microsoft.dagx.common.annotations.IntegrationTest;
import com.microsoft.dagx.common.testfixtures.AbstractS3Test;
import com.microsoft.dagx.common.testfixtures.DagxExtension;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.dagx.common.testfixtures.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@ExtendWith(DagxExtension.class)
public class PutS3ObjectTest extends AbstractS3Test {

    @Test
    void upload() throws IOException {
        String key = "test-file.txt";
        TestRunner runner = TestRunners.newTestRunner(new PutS3Object());

        runner.setProperty(Properties.REGION, region);
        runner.setProperty(Properties.BUCKET, bucketName);
        runner.setProperty(Properties.OBJECT_KEYS, key);
        runner.setProperty(Properties.ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
        runner.setProperty(Properties.SECRET_ACCESS_KEY, credentials.getAWSSecretKey());


        Map<String, String> attrs = new HashMap<>();
        runner.enqueue(getResourcePath(SAMPLE_FILE_RESOURCE_NAME), attrs);
        runner.enqueue(getResourcePath(SAMPLE_FILE_RESOURCE_NAME), attrs);
        runner.enqueue(getResourcePath(SAMPLE_FILE_RESOURCE_NAME), attrs);

        runner.run(3);

        runner.assertAllFlowFilesTransferred(Properties.REL_SUCCESS, 3);
    }

    @Test
    void upload_whenAlreadyExists() throws IOException {
        String key = "test-file.txt";
        PutObjectResult response = putTestFile(key, getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME), bucketName);

        TestRunner runner = TestRunners.newTestRunner(new PutS3Object());

        runner.setProperty(Properties.REGION, region);
        runner.setProperty(Properties.BUCKET, bucketName);
        runner.setProperty(Properties.OBJECT_KEYS, key);
        runner.setProperty(Properties.ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
        runner.setProperty(Properties.SECRET_ACCESS_KEY, credentials.getAWSSecretKey());


        Map<String, String> attrs = new HashMap<>();
        String newContent = "this is another content!";
        runner.enqueue(newContent, attrs);

        runner.run(1);

        runner.assertAllFlowFilesTransferred(Properties.REL_SUCCESS, 1);

        S3Object newS3Object = fetchTestFile(bucketName, key);
        assertThat(new String(newS3Object.getObjectContent().readAllBytes())).isEqualTo(newContent);
        assertThat(newS3Object.getObjectMetadata().getETag()).isNotEqualTo(response.getETag());
    }

}