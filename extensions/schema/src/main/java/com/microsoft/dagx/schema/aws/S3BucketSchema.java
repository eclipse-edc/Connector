/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.schema.aws;


import com.microsoft.dagx.schema.DataSchema;
import com.microsoft.dagx.schema.SchemaAttribute;

public class S3BucketSchema extends DataSchema {

    public static final String TYPE = "dagx:s3";
    public static String REGION = "region";
    public static String BUCKET_NAME = "bucketName";

    @Override
    protected void addAttributes() {
        attributes.add(new SchemaAttribute("region", true));
        attributes.add(new SchemaAttribute("bucketName", true));
    }

    @Override
    public String getName() {
        return TYPE;
    }
}
