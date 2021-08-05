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

package org.eclipse.dataspaceconnector.transfer.nifi.processors;

import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.eclipse.dataspaceconnector.aws.testfixtures.AbstractS3Test;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.common.testfixtures.EdcExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.SAMPLE_FILE_RESOURCE_NAME;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFileFromResourceName;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getResourcePath;

@IntegrationTest
@ExtendWith(EdcExtension.class)
class PutS3ObjectTest extends AbstractS3Test {

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
