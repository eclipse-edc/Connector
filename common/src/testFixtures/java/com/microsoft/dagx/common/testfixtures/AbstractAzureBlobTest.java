/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.common.testfixtures;

import com.azure.core.credential.AzureSasCredential;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

import static com.microsoft.dagx.common.ConfigurationFunctions.propOrEnv;
import static org.junit.jupiter.api.Assertions.fail;

public class AbstractAzureBlobTest {

    protected static final String accountName = "dagxblobstoreitest";
    protected static BlobServiceClient blobServiceClient;
    protected String containerName;
    protected boolean reuseClient = true;
    protected String testRunId;

    @BeforeEach
    public void setupClient() {

        testRunId = UUID.randomUUID().toString();
        containerName = "fetch-azure-processor-" + testRunId;

        if (blobServiceClient == null || !reuseClient) {
            final String accountSas = getSasToken();
            blobServiceClient = new BlobServiceClientBuilder().credential(new AzureSasCredential(accountSas)).endpoint("https://" + accountName + ".blob.core.windows.net").buildClient();
        }

        if (blobServiceClient.getBlobContainerClient(containerName).exists()) {
            fail("Container " + containerName + " already exists - tests  will fail!");
        }

        //create container
        BlobContainerClient blobContainerClient = blobServiceClient.createBlobContainer(containerName);
        if (!blobContainerClient.exists()) {
            fail("Setup incomplete, tests will fail");

        }
    }

    @AfterEach
    public void teardown() {
        try {
            blobServiceClient.deleteBlobContainer(containerName);
        } catch (Exception ex) {
            fail("teardown failed, subsequent tests might fail as well!");
        }
    }

    @NotNull
    protected String getSasToken() {
        return Objects.requireNonNull(propOrEnv("AZ_STORAGE_SAS", null), "AZ_STORAGE_SAS");
    }

    protected void putBlob(String name, File file) {
        blobServiceClient.getBlobContainerClient(containerName)
                .getBlobClient(name)
                .uploadFromFile(file.getAbsolutePath(), true);
    }
}
