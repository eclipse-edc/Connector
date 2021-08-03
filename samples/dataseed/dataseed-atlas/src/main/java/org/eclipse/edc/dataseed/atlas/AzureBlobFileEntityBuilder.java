/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.edc.dataseed.atlas;

import org.eclipse.edc.catalog.atlas.metadata.AtlasCustomTypeAttribute;
import org.eclipse.edc.schema.azure.*;

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
    private String policy;

    private AzureBlobFileEntityBuilder() {
    }

    public static AzureBlobFileEntityBuilder newInstance() {
        return new AzureBlobFileEntityBuilder();
    }

    public AzureBlobFileEntityBuilder account(String account) {
        this.account = account;
        return this;
    }

    public AzureBlobFileEntityBuilder blobName(String blobname) {
        blobName = blobname;
        return this;
    }

    public AzureBlobFileEntityBuilder container(String container) {
        this.container = container;
        return this;
    }

    public AzureBlobFileEntityBuilder keyName(String keyName) {
        this.keyName = keyName;
        return this;
    }

    public AzureBlobFileEntityBuilder description(String description) {
        this.description = description;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> azureBlobFileEntity = new HashMap<>();
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.EDC_STORAGE_KEYNAME, keyName);
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.EDC_STORAGE_TYPE, AzureBlobStoreSchema.TYPE);
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.AZURE_BLOB_CONTAINER, container);
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.AZURE_BLOB_ACCOUNT, account);
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.AZURE_BLOB_BLOBNAME, blobName);
        azureBlobFileEntity.put("policyId", policy);

        //the following properties are required by atlas
        azureBlobFileEntity.put("qualifiedName", format("%s/%s/%s", account, container, blobName));
        azureBlobFileEntity.put("name", blobName);
        azureBlobFileEntity.put("description", description);
        return azureBlobFileEntity;
    }

    public AzureBlobFileEntityBuilder policy(String policyId) {
        policy = policyId;
        return this;
    }
}
