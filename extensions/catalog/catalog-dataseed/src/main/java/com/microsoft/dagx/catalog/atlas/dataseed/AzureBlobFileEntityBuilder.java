package com.microsoft.dagx.catalog.atlas.dataseed;

import com.microsoft.dagx.catalog.atlas.metadata.AtlasCustomTypeAttribute;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;


public final class AzureBlobFileEntityBuilder {
    private String account;
    private String blobName;
    private String container;
    private String keyName;
    private String description;

    public static final String ENTITY_TYPE_NAME = "AzureBlobFile";

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
        this.blobName = blobname;
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

    public AzureBlobFileEntityBuilder withDescription(String description){
        this.description= description;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> azureBlobFileEntity = new HashMap<>();
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.DAGX_STORAGE_KEYNAME, this.keyName);
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.DAGX_STORAGE_TYPE, "AzureStorage");
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.AZURE_BLOB_CONTAINER, this.container);
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.AZURE_BLOB_ACCOUNT, this.account);
        azureBlobFileEntity.put(AtlasCustomTypeAttribute.AZURE_BLOB_BLOBNAME, this.blobName);

        //the following properties are required by atlas
        azureBlobFileEntity.put("qualifiedName", format("%s/%s/%s", this.account, container, blobName));
        azureBlobFileEntity.put("name", blobName);
        azureBlobFileEntity.put("description", description);
        return azureBlobFileEntity;
    }
}
