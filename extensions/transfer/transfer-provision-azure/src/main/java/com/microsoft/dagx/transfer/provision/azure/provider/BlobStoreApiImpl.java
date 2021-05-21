/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.provision.azure.provider;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.microsoft.dagx.spi.security.Vault;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BlobStoreApiImpl implements BlobStoreApi {

    private final Vault vault;
    private final Map<String, BlobServiceClient> cache = new HashMap<>();

    public BlobStoreApiImpl(Vault vault) {
        this.vault = vault;
    }


    @Override
    public void createContainer(String accountName, String containerName) {
        getBlobServiceClient(accountName).createBlobContainer(containerName);
    }

    @Override
    public void deleteContainer(String accountName, String containerName) {
        getBlobServiceClient(accountName).deleteBlobContainer(containerName);
    }

    @Override
    public boolean exists(String accountName, String containerName) {
        return getBlobServiceClient(accountName).getBlobContainerClient(containerName).exists();
    }

    @Override
    public String createContainerSasToken(String accountName, String containerName, String oermissionSpec, OffsetDateTime expiry) {
        BlobContainerSasPermission permissions = BlobContainerSasPermission.parse(oermissionSpec);
        BlobServiceSasSignatureValues vals = new BlobServiceSasSignatureValues(expiry, permissions);
        return getBlobServiceClient(accountName).getBlobContainerClient(containerName).generateSas(vals);
    }

    private BlobServiceClient getBlobServiceClient(String accountName) {
        Objects.requireNonNull(accountName, "accountName");

        if (cache.containsKey(accountName)) {
            return cache.get(accountName);
        }


        var accountKey = vault.resolveSecret(accountName + "-key1");

        if (accountKey == null) {
            throw new IllegalArgumentException("No Object Storage credential found in vault!");
        }

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().credential(createCredential(accountKey, accountName))
                .endpoint(createEndpoint(accountName))
                .buildClient();

        cache.put(accountKey, blobServiceClient);
        return blobServiceClient;
    }


    private String createEndpoint(String accountName) {

        return "https://" + accountName + ".blob.core.windows.net";
    }

    private StorageSharedKeyCredential createCredential(String accountKey, String accountName) {
        return new StorageSharedKeyCredential(accountName, accountKey);
    }


}
