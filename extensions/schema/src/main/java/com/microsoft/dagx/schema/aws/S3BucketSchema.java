package com.microsoft.dagx.schema.aws;


import com.microsoft.dagx.schema.Schema;
import com.microsoft.dagx.schema.SchemaAttribute;

public class S3BucketSchema extends Schema {

    @Override
    protected void addAttributes() {
        attributes.add(new SchemaAttribute("region", true));
        attributes.add(new SchemaAttribute("bucketName", true));
    }

    @Override
    public String getName() {
        return "S3Bucket";
    }
}
