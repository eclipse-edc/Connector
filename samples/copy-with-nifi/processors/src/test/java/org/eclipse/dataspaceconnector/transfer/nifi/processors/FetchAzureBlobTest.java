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

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.eclipse.dataspaceconnector.azure.testfixtures.AbstractAzureBlobTest;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.SAMPLE_FILE_RESOURCE_NAME;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFileFromResourceName;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getResourcePath;

@IntegrationTest
@ExtendWith(EdcExtension.class)
class FetchAzureBlobTest extends AbstractAzureBlobTest {

    private TestRunner runner;
    private String EXPECTED_CONTENT;

    @BeforeEach
    void setup() throws IOException {
        runner = TestRunners.newTestRunner(new FetchAzureBlob());
        runner.setProperty(Properties.ACCOUNT_NAME, accountName);
        runner.setProperty(Properties.SAS_TOKEN, getSasToken());
        EXPECTED_CONTENT = Files.readString(getResourcePath(SAMPLE_FILE_RESOURCE_NAME));
    }

    @Test
    void testFetch_single() {
        String name = "hello.txt";
        File fileFromResourceName = getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME);
        putBlob(name, fileFromResourceName);

        runner.setProperty(Properties.OBJECT_KEYS, name);
        runner.setProperty(Properties.CONTAINER, containerName);
        runner.assertValid();

        runner.enqueue(new byte[0]);
        runner.run();

        runner.assertAllFlowFilesTransferred(Properties.REL_SUCCESS, 1);
        List<MockFlowFile> flowFilesForRelationship = runner.getFlowFilesForRelationship(Properties.REL_SUCCESS);
        assertThat(flowFilesForRelationship).hasSize(1);

        for (MockFlowFile flowFile : flowFilesForRelationship) {
            flowFile.assertContentEquals(EXPECTED_CONTENT);
            flowFile.assertAttributeEquals("azure.length", String.valueOf(EXPECTED_CONTENT.length()));
        }
    }

    @Test
    void testFetch_containerNotExist() {
        String name = "hello.txt";
        File fileFromResourceName = getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME);
        putBlob(name, fileFromResourceName);

        runner.setProperty(Properties.OBJECT_KEYS, name);
        runner.setProperty(Properties.CONTAINER, "notexist");
        runner.assertValid();

        runner.enqueue(new byte[0]);
        runner.run();

        runner.assertAllFlowFilesTransferred(Properties.REL_FAILURE, 1);
    }

    @Test
    @Disabled
        //takes very long
    void testFetch_accountNotExist() {
        String name = "hello.txt";
        File fileFromResourceName = getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME);
        putBlob(name, fileFromResourceName);

        runner.setProperty(Properties.OBJECT_KEYS, name);
        runner.setProperty(Properties.CONTAINER, containerName);
        runner.setProperty(Properties.ACCOUNT_NAME, "non-exist-account");

        runner.assertValid();

        runner.enqueue(new byte[0]);
        assertThatThrownBy(() -> runner.run()).hasRootCauseInstanceOf(UnknownHostException.class);

    }

    @Test
    void testFetch_muliple_allExist() {
        String name = "hello.txt";
        putBlob(name, getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME));

        String name2 = "hello2.txt";
        putBlob(name2, getFileFromResourceName(name2));


        runner.setProperty(Properties.OBJECT_KEYS, "[\"" + name + "\", \"" + name2 + "\"]");
        runner.setProperty(Properties.CONTAINER, containerName);
        runner.assertValid();

        runner.enqueue(new byte[0]);
        runner.run();

        runner.assertAllFlowFilesTransferred(Properties.REL_SUCCESS, 2);
        List<MockFlowFile> flowFilesForRelationship = runner.getFlowFilesForRelationship(Properties.REL_SUCCESS);
        assertThat(flowFilesForRelationship).hasSize(2);
        assertThat(flowFilesForRelationship).allSatisfy(mff -> assertThat(mff.getAttribute("filename")).contains("hello"));
    }

    @Test
    void testFetch_multiple_someExist_shouldEnumerate() {
        String name = "hello.txt";
        putBlob(name, getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME));

        String name2 = "hello2.txt";


        runner.setProperty(Properties.OBJECT_KEYS, "[\"" + name + "\", \"" + name2 + "\"]");
        runner.setProperty(Properties.CONTAINER, containerName);
        runner.assertValid();

        runner.enqueue(new byte[0]);
        runner.run();

        List<MockFlowFile> successFulFiles = runner.getFlowFilesForRelationship(Properties.REL_SUCCESS);
        assertThat(successFulFiles).hasSize(1);
        successFulFiles.get(0).assertContentEquals(EXPECTED_CONTENT);
        assertThat(successFulFiles).allSatisfy(mff -> assertThat(mff.getAttribute("filename")).contains("hello"));

        var failedFiles = runner.getFlowFilesForRelationship(Properties.REL_FAILURE);
        assertThat(failedFiles).hasSize(1);

    }

    @Test
    void testFetch_none_enumerates() {
        String name = "hello.txt";
        putBlob(name, getFileFromResourceName(SAMPLE_FILE_RESOURCE_NAME));

        String name2 = "hello2.txt";
        putBlob(name2, getFileFromResourceName(name2));


        runner.setProperty(Properties.CONTAINER, containerName);
        runner.assertValid();

        runner.enqueue(new byte[0]);
        runner.run();

        runner.assertAllFlowFilesTransferred(Properties.REL_SUCCESS, 2);
        List<MockFlowFile> flowFilesForRelationship = runner.getFlowFilesForRelationship(Properties.REL_SUCCESS);
        assertThat(flowFilesForRelationship).hasSize(2);
        assertThat(flowFilesForRelationship).allSatisfy(mff -> assertThat(mff.getAttribute("filename")).contains("hello"));

    }


}
