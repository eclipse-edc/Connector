/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.schema.azure;

import com.microsoft.dagx.schema.Schema;
import com.microsoft.dagx.schema.SchemaAttribute;

public class AzureBlobStoreSchema extends Schema {


    public final static String TYPE = "AzureStorage";

    @Override
    protected void addAttributes() {
        attributes.add(new SchemaAttribute("account", true));
        attributes.add(new SchemaAttribute("blobname", true));
        attributes.add(new SchemaAttribute("container", true));
    }


    @Override
    public String getName() {
        return TYPE;
    }

}
