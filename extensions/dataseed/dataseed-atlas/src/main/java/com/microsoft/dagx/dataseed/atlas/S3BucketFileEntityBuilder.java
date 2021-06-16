/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.dataseed.atlas;

import com.microsoft.dagx.catalog.atlas.metadata.AtlasCustomTypeAttribute;
import com.microsoft.dagx.schema.aws.S3BucketSchema;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class S3BucketFileEntityBuilder {

    private String bucket;
    private String region;
    private String keyName;
    private String name;
    private String description;
    private String policy;

    public static S3BucketFileEntityBuilder newInstance() {
        return new S3BucketFileEntityBuilder();
    }

    public S3BucketFileEntityBuilder bucket(String bucket) {
        this.bucket = bucket;
        return this;
    }

    public S3BucketFileEntityBuilder region(String region) {
        this.region = region;
        return this;
    }

    public S3BucketFileEntityBuilder keyname(String keyName) {
        this.keyName = keyName;
        return this;
    }

    public S3BucketFileEntityBuilder name(String name) {
        this.name = name;
        return this;
    }

    public S3BucketFileEntityBuilder description(String description) {
        this.description = description;
        return this;
    }

    public S3BucketFileEntityBuilder policy(String policyId) {
        policy = policyId;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> s3Entity = new HashMap<>();
        s3Entity.put(AtlasCustomTypeAttribute.DAGX_STORAGE_KEYNAME, keyName);
        s3Entity.put(AtlasCustomTypeAttribute.DAGX_STORAGE_TYPE, S3BucketSchema.TYPE);
        s3Entity.put("bucketName", bucket);
        s3Entity.put("region", region);
        s3Entity.put("policyId", policy);

        //the following properties are required by atlas
        s3Entity.put("qualifiedName", format("%s/%s/%s", region, bucket, name));
        s3Entity.put("name", name);
        s3Entity.put("description", description);
        return s3Entity;
    }
}
