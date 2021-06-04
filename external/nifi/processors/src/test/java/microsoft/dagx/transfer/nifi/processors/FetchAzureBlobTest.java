/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package microsoft.dagx.transfer.nifi.processors;

import com.azure.core.credential.AzureSasCredential;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.microsoft.dagx.common.ConfigurationFunctions.propOrEnv;
import static microsoft.dagx.transfer.nifi.processors.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
class FetchAzureBlobTest {

    protected static final String accountName = "dagxblobstoreitest";
    protected static final String containerName = "fetch-azure-processor-" + UUID.randomUUID();
    private static BlobServiceClient blobServiceClient;
    private TestRunner runner;
    private String EXPECTED_CONTENT;

    @BeforeAll
    public static void oneTimeSetup() {
        //         this is necessary because the @EnabledIf... annotation does not prevent @BeforeAll to be called
        var isCi = propOrEnv("CI", "false");
        if (!Boolean.parseBoolean(isCi)) {
            return;
        }

        final String accountSas = getSasToken();
        blobServiceClient = new BlobServiceClientBuilder().credential(new AzureSasCredential(accountSas)).endpoint("https://" + accountName + ".blob.core.windows.net").buildClient();

        if (blobServiceClient.getBlobContainerClient(containerName).exists()) {
            fail("Container " + containerName + " already exists - tests  will fail!");
        }

        //create container
        BlobContainerClient blobContainerClient = blobServiceClient.createBlobContainer(containerName);
        if (!blobContainerClient.exists()) {
            fail("Setup incomplete, tests will fail");

        }
    }


    @AfterAll
    public static void oneTtimeTeardown() {
        try {
            blobServiceClient.deleteBlobContainer(containerName);
        } catch (Exception ex) {
            fail("teardown failed, subsequent tests might fail as well!");
        }
    }

    @NotNull
    private static String getSasToken() {
        return Objects.requireNonNull(propOrEnv("AZ_STORAGE_SAS", null), "AZ_STORAGE_SAS");
    }

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

    private void putBlob(String name, File file) {
        blobServiceClient.getBlobContainerClient(containerName)
                .getBlobClient(name)
                .uploadFromFile(file.getAbsolutePath(), true);
    }
}
