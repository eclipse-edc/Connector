/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.schema.azure;

import com.microsoft.dagx.schema.DataSchema;
import com.microsoft.dagx.schema.SchemaAttribute;

public class AzureBlobStoreSchema extends DataSchema {


    public final static String TYPE = "AzureStorage";
    public static final String CONTAINER_NAME = "container";
    public static final String ACCOUNT_NAME = "account";
    public static final String BLOB_NAME = "blobname";

    @Override
    protected void addAttributes() {
        attributes.add(new SchemaAttribute(ACCOUNT_NAME, true));
        attributes.add(new SchemaAttribute(BLOB_NAME, false));
        attributes.add(new SchemaAttribute(CONTAINER_NAME, true));
    }


    @Override
    public String getName() {
        return TYPE;
    }

}
