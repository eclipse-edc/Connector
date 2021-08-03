/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.schema.s3;


import org.eclipse.dataspaceconnector.schema.DataSchema;
import org.eclipse.dataspaceconnector.schema.SchemaAttribute;

public class S3BucketSchema extends DataSchema {

    public static final String TYPE = "AmazonS3";
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
