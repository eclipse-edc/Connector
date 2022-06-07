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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractAzureBlobTest {

    protected final String account1Name = "account1";
    protected final String account1Key = "key1";
    protected final String account2Name = "account2";
    protected final String account2Key = "key2";
    protected BlobServiceClient blobServiceClient1;
    protected BlobServiceClient blobServiceClient2;
    protected String account1ContainerName;
    protected List<Runnable> containerCleanup = new ArrayList<>();
    protected String testRunId = UUID.randomUUID().toString();

    @BeforeEach
    public void setupClient() {
        account1ContainerName = "storage-container-" + testRunId;

        blobServiceClient1 = TestFunctions.getBlobServiceClient(account1Name, account1Key);
        blobServiceClient2 = TestFunctions.getBlobServiceClient(account2Name, account2Key);

        createContainer(blobServiceClient1, account1ContainerName);
    }

    protected void createContainer(BlobServiceClient client, String containerName) {
        assertFalse(client.getBlobContainerClient(containerName).exists());

        BlobContainerClient blobContainerClient = client.createBlobContainer(containerName);
        assertTrue(blobContainerClient.exists());
        containerCleanup.add(() -> client.deleteBlobContainer(containerName));
    }

    @AfterEach
    public void teardown() {
        for (var cleanup : containerCleanup) {
            try {
                cleanup.run();
            } catch (Exception ex) {
                fail("teardown failed, subsequent tests might fail as well!");
            }
        }
    }

    protected void putBlob(String name, File file) {
        blobServiceClient1.getBlobContainerClient(account1ContainerName)
                .getBlobClient(name)
                .uploadFromFile(file.getAbsolutePath(), true);
    }
}
