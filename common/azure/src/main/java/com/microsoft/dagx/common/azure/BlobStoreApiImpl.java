/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.common.azure;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.sas.AccountSasPermission;
import com.azure.storage.common.sas.AccountSasResourceType;
import com.azure.storage.common.sas.AccountSasService;
import com.azure.storage.common.sas.AccountSasSignatureValues;
import com.microsoft.dagx.spi.security.Vault;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Override
    public List<BlobItem> listContainer(String accountName, String containerName) {
        return getBlobServiceClient(accountName).getBlobContainerClient(containerName).listBlobs().stream().collect(Collectors.toList());
    }

    @Override
    public void putBlob(String accountName, String containerName, String blobName, byte[] data) {
        final BlobServiceClient blobServiceClient = getBlobServiceClient(accountName);
        blobServiceClient.getBlobContainerClient(containerName).getBlobClient(blobName).upload(BinaryData.fromBytes(data), true);
    }

    @Override
    public String createAccountSas(String accountName, String containerName, String permissionSpec, OffsetDateTime expiry) {
        AccountSasPermission permissions = AccountSasPermission.parse(permissionSpec);

        AccountSasService services = AccountSasService.parse("b");
        AccountSasResourceType resourceTypes = AccountSasResourceType.parse("co");
        AccountSasSignatureValues vals = new AccountSasSignatureValues(expiry, permissions, services, resourceTypes);
        return getBlobServiceClient(accountName).generateAccountSas(vals);
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

        cache.put(accountName, blobServiceClient);
        return blobServiceClient;
    }


    private String createEndpoint(String accountName) {

        return "https://" + accountName + ".blob.core.windows.net";
    }

    private StorageSharedKeyCredential createCredential(String accountKey, String accountName) {
        return new StorageSharedKeyCredential(accountName, accountKey);
    }


}
