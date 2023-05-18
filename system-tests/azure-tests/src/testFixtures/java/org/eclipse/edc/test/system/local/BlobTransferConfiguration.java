/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.test.system.local;

import com.azure.storage.blob.BlobServiceClient;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.test.system.utils.TransferConfiguration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_ASSET_FILE;

public class BlobTransferConfiguration implements TransferConfiguration {

    static final String BLOB_CONTENT = "Test blob content";
    private final String consumerManagementUrl;
    private final String providerIdsUrl;
    private final BlobServiceClient blobServiceClient;
    private final int maxSeconds;

    public BlobTransferConfiguration(String consumerManagementUrl, String providerIdsUrl, BlobServiceClient blobServiceClient, int maxSeconds) {
        this.consumerManagementUrl = consumerManagementUrl;
        this.providerIdsUrl = providerIdsUrl;
        this.blobServiceClient = blobServiceClient;
        this.maxSeconds = maxSeconds;
    }

    @Override
    public String getConsumerManagementUrl() {
        return consumerManagementUrl;
    }

    @Override
    public String getProviderIdsUrl() {
        return providerIdsUrl;
    }

    @Override
    public JsonObject createBlobDestination() {
        return Json.createObjectBuilder()
                .add("type", AzureBlobStoreSchema.TYPE)
                .add("properties", Json.createObjectBuilder()
                        .add(ACCOUNT_NAME, blobServiceClient.getAccountName()))
                .build();

    }

    @Override
    public boolean isTransferResultValid(Map<String, String> dataDestinationProperties) {
        // Assert
        var container = dataDestinationProperties.get("edc:container");
        var destinationBlob = blobServiceClient.getBlobContainerClient(container)
                .getBlobClient(PROVIDER_ASSET_FILE);
        assertThat(destinationBlob.exists())
                .withFailMessage("Destination blob %s not created", destinationBlob.getBlobUrl())
                .isTrue();
        var actualBlobContent = destinationBlob.downloadContent().toString();
        assertThat(actualBlobContent)
                .withFailMessage("Transferred file contents are not same as the source file")
                .isEqualTo(BLOB_CONTENT);
        return true;
    }
}
