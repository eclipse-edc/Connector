/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.edc.dataseed.azure;

import org.eclipse.edc.common.azure.BlobStoreApi;
import org.eclipse.edc.provision.azure.AzureSasToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.security.VaultResponse;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Set;

public class AzureBlobDataseedExtension implements ServiceExtension {

    private static final String ACCOUNT_NAME = "edctfblob";
    private static final String CONTAINER_NAME = "src-container";
    private static final String TEST_FILE_NAME = "donald.png";
    private static final String TEST_FILE_NAME2 = "donald.png";
    private BlobStoreApi blobStoreApi;
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {

        blobStoreApi = context.getService(BlobStoreApi.class);
        monitor = context.getMonitor();


        if (!blobStoreApi.exists(AzureBlobDataseedExtension.ACCOUNT_NAME, AzureBlobDataseedExtension.CONTAINER_NAME)) {
            monitor.info("Azure DataSeed: creating container '" + AzureBlobDataseedExtension.CONTAINER_NAME + "'");
            blobStoreApi.createContainer(AzureBlobDataseedExtension.ACCOUNT_NAME, AzureBlobDataseedExtension.CONTAINER_NAME);
        } else {
            monitor.info("Azure DataSeed: container '" + AzureBlobDataseedExtension.CONTAINER_NAME + "' exists - will reuse it.");
        }

        monitor.info("Azure DataSeed: uploading a few test files");
        var is = getClass().getClassLoader().getResourceAsStream(AzureBlobDataseedExtension.TEST_FILE_NAME);
        uploadFile(is, AzureBlobDataseedExtension.TEST_FILE_NAME, "testimage.jpg");

        var is2 = getClass().getClassLoader().getResourceAsStream(AzureBlobDataseedExtension.TEST_FILE_NAME2);
        uploadFile(is2, AzureBlobDataseedExtension.TEST_FILE_NAME2, "anotherimage.jpg");

        monitor.info("Azure DataSeed: create SAS Token for container");
        OffsetDateTime expiry = OffsetDateTime.now().plusMonths(1L);
        String sasToken = "?" + blobStoreApi.createAccountSas(AzureBlobDataseedExtension.ACCOUNT_NAME, AzureBlobDataseedExtension.CONTAINER_NAME, "racwxdl", expiry);
        AzureSasToken token = new AzureSasToken(sasToken, expiry.toInstant().toEpochMilli());

        var vault = context.getService(Vault.class);
        VaultResponse vaultResponse = vault.storeSecret(AzureBlobDataseedExtension.CONTAINER_NAME, context.getTypeManager().writeValueAsString(token));

        if (vaultResponse.success()) {
            monitor.info("Azure DataSeed: new temporary SAS token stored for " + AzureBlobDataseedExtension.CONTAINER_NAME);
        } else {
            monitor.severe("Azure DataSeed: error when storing temporary SAS token: " + vaultResponse.error());
        }
    }

    private void uploadFile(InputStream is, String filename, String asBlobName) {
        try {
            if (is != null) {
                monitor.info("Azure DataSeed: uploading test file '" + filename + "' as blob name '" + asBlobName + "'");
                blobStoreApi.putBlob(AzureBlobDataseedExtension.ACCOUNT_NAME, AzureBlobDataseedExtension.CONTAINER_NAME, asBlobName, is.readAllBytes());
            } else {
                monitor.severe("Azure DataSeed: test file '" + filename + "' does not exist!");
            }
        } catch (IOException e) {
            monitor.severe("Azure DataSeed: error reading test file '" + filename + "'!", e);
        }
    }

    @Override
    public Set<String> requires() {
        return Set.of("edc:blobstoreapi");
    }
}
