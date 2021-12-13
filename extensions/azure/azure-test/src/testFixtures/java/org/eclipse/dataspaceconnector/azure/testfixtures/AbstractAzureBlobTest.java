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

package org.eclipse.dataspaceconnector.azure.testfixtures;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractAzureBlobTest {

    protected final String accountName = "account1";
    protected final String accountKey = "key1";
    protected final String endpoint = "http://127.0.0.1:10000/" + accountName;
    protected BlobServiceClient blobServiceClient;
    protected String containerName;
    protected String testRunId = UUID.randomUUID().toString();

    @BeforeEach
    public void setupClient() {
        containerName = "storage-container-" + testRunId;

        blobServiceClient = new BlobServiceClientBuilder()
                .credential(new StorageSharedKeyCredential(accountName, accountKey))
                .endpoint(endpoint)
                .buildClient();

        assertFalse(blobServiceClient.getBlobContainerClient(containerName).exists());

        BlobContainerClient blobContainerClient = blobServiceClient.createBlobContainer(containerName);
        assertTrue(blobContainerClient.exists());
    }

    @AfterEach
    public void teardown() {
        try {
            blobServiceClient.deleteBlobContainer(containerName);
        } catch (Exception ex) {
            fail("teardown failed, subsequent tests might fail as well!");
        }
    }

    protected void putBlob(String name, File file) {
        blobServiceClient.getBlobContainerClient(containerName)
                .getBlobClient(name)
                .uploadFromFile(file.getAbsolutePath(), true);
    }
}
