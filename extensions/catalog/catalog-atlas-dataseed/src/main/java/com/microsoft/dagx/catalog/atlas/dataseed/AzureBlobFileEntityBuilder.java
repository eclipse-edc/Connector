/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.catalog.atlas.dataseed;

import com.microsoft.dagx.catalog.atlas.metadata.AtlasCustomTypeAttribute;
import com.microsoft.dagx.schema.azure.AzureBlobStoreSchema;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;


public final class AzureBlobFileEntityBuilder {
    public static final String ENTITY_TYPE_NAME = AzureBlobStoreSchema.TYPE;
    private String account;
    private String blobName;
    private String container;
    private String keyName;
    private String description;

    private AzureBlobFileEntityBuilder() {
    }

    public static AzureBlobFileEntityBuilder newInstance() {
        return new AzureBlobFileEntityBuilder();
    }

    public AzureBlobFileEntityBuilder withAccount(String account) {
        this.account = account;
        return this;
    }

    public AzureBlobFileEntityBuilder withBlobname(String blobname) {
        blobName = blobname;
        return this;
    }

    public AzureBlobFileEntityBuilder withContainer(String container) {
        this.container = container;
        return this;
    }

    public AzureBlobFileEntityBuilder withKeyName(String keyName) {
        this.keyName = keyName;
        return this;
    }

    public AzureBlobFileEntityBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> azureBlobFileEntity = new HashMap<>();
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.DAGX_STORAGE_KEYNAME, keyName);
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.DAGX_STORAGE_TYPE, AzureBlobStoreSchema.TYPE);
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.AZURE_BLOB_CONTAINER, container);
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.AZURE_BLOB_ACCOUNT, account);
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.AZURE_BLOB_BLOBNAME, blobName);

        //the following properties are required by atlas
        azureBlobFileEntity.put("qualifiedName", format("%s/%s/%s", account, container, blobName));
        azureBlobFileEntity.put("name", blobName);
        azureBlobFileEntity.put("description", description);
        return azureBlobFileEntity;
    }
}
