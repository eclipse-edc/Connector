/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.dataseed.azure;

import com.microsoft.dagx.common.azure.BlobStoreApi;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.security.VaultResponse;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.transfer.provision.azure.AzureSasToken;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Set;

public class AzureBlobDataseedExtension implements ServiceExtension {

    private static final String ACCOUNT_NAME = "dagxtfblob";
    private static final String CONTAINER_NAME = "src-container";
    private static final String TEST_FILE_NAME = "donald.png";
    private static final String TEST_FILE_NAME2 = "donald.png";
    private BlobStoreApi blobStoreApi;
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {

        blobStoreApi = context.getService(BlobStoreApi.class);
        monitor = context.getMonitor();


        if (!blobStoreApi.exists(ACCOUNT_NAME, CONTAINER_NAME)) {
            monitor.info("Azure DataSeed: creating container '" + CONTAINER_NAME + "'");
            blobStoreApi.createContainer(ACCOUNT_NAME, CONTAINER_NAME);
        } else {
            monitor.info("Azure DataSeed: container '" + CONTAINER_NAME + "' exists - will reuse it.");
        }

        monitor.info("Azure DataSeed: uploading a few test files");
        var is = getClass().getClassLoader().getResourceAsStream(TEST_FILE_NAME);
        uploadFile(is, TEST_FILE_NAME, "testimage.jpg");

        var is2 = getClass().getClassLoader().getResourceAsStream(TEST_FILE_NAME2);
        uploadFile(is2, TEST_FILE_NAME2, "anotherimage.jpg");

        monitor.info("Azure DataSeed: create SAS Token for container");
        final OffsetDateTime expiry = OffsetDateTime.now().plusMonths(1L);
        final String sasToken = "?" + blobStoreApi.createAccountSas(ACCOUNT_NAME, CONTAINER_NAME, "racwxdl", expiry);
        AzureSasToken token = new AzureSasToken(sasToken, expiry.toInstant().toEpochMilli());

        var vault = context.getService(Vault.class);
        final VaultResponse vaultResponse = vault.storeSecret(CONTAINER_NAME, context.getTypeManager().writeValueAsString(token));

        if (vaultResponse.success()) {
            monitor.info("Azure DataSeed: new temporary SAS token stored for " + CONTAINER_NAME);
        } else {
            monitor.severe("Azure DataSeed: error when storing temporary SAS token: " + vaultResponse.error());
        }
    }

    private void uploadFile(InputStream is, String filename, String asBlobName) {
        try {
            if (is != null) {
                monitor.info("Azure DataSeed: uploading test file '" + filename + "' as blob name '" + asBlobName + "'");
                blobStoreApi.putBlob(ACCOUNT_NAME, CONTAINER_NAME, asBlobName, is.readAllBytes());
            } else {
                monitor.severe("Azure DataSeed: test file '" + filename + "' does not exist!");
            }
        } catch (IOException e) {
            monitor.severe("Azure DataSeed: error reading test file '" + filename + "'!", e);
        }
    }

    @Override
    public Set<String> requires() {
        return Set.of("dagx:blobstoreapi");
    }
}
