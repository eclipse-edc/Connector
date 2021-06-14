/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.demo.file.zip.schema;

import com.microsoft.dagx.schema.DataSchema;
import com.microsoft.dagx.schema.SchemaAttribute;

public class ZipSchema extends DataSchema {

    public static final String TYPE = "zip";
    public static String DIRECTORY = "directory";
    public static String NAME = "name";

    @Override
    protected void addAttributes() {
        attributes.add(new SchemaAttribute(DIRECTORY, true));
        attributes.add(new SchemaAttribute(NAME, true));
    }

    @Override
    public String getName() {
        return TYPE;
    }

}