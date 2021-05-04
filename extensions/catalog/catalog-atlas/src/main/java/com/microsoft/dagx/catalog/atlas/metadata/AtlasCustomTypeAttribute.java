package com.microsoft.dagx.catalog.atlas.metadata;

import com.microsoft.dagx.schema.SchemaAttribute;

import java.util.ArrayList;
import java.util.List;

public class AtlasCustomTypeAttribute extends SchemaAttribute {
    public static final String ATLAS_TYPE_STRING = "string";
    public static final String AZURE_BLOB_ACCOUNT = "account";
    public static final String AZURE_BLOB_BLOBNAME = "blobname";
    public static final String AZURE_BLOB_CONTAINER = "container";
    public static final String DAGX_STORAGE_TYPE = "type";
    public static final String DAGX_STORAGE_KEYNAME = "keyName";

    public AtlasCustomTypeAttribute(String name, String type, boolean required) {
        super(name, type, required);
    }

    public AtlasCustomTypeAttribute() {
        super();
    }


}
