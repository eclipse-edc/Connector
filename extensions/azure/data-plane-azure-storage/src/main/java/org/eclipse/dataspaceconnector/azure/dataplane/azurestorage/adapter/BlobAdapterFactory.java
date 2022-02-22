/*
 *  Copyright (c) 2022 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter;

import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.common.StorageSharedKeyCredential;

import java.util.Objects;

/**
 * Factory class for {@link BlobAdapter}.
 */
public class BlobAdapterFactory {
    private final String blobstoreEndpoint;

    public BlobAdapterFactory(String blobstoreEndpoint) {
        this.blobstoreEndpoint = blobstoreEndpoint;
    }

    public BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, String sharedKey) {
        var blobServiceClient = new BlobServiceClientBuilder()
                .credential(new StorageSharedKeyCredential(accountName, sharedKey))
                .endpoint(createEndpoint(accountName))
                .buildClient();

        var blockBlobClient = blobServiceClient
                .getBlobContainerClient(containerName)
                .getBlobClient(blobName)
                .getBlockBlobClient();

        return new DefaultBlobAdapter(blockBlobClient);
    }

    private String createEndpoint(String accountName) {
        return Objects.requireNonNullElseGet(blobstoreEndpoint, () -> "https://" + accountName + ".blob.core.windows.net");
    }

}

