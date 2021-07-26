package com.microsoft.dagx.schema.s3;


import com.microsoft.dagx.schema.RelationshipSchema;
import com.microsoft.dagx.schema.policy.PolicySchema;

public class AmazonS3HasPolicyRelationshipSchema extends RelationshipSchema {
    public static final String TYPE = "dagx:schema:relation:amazons3_has_policy";


    @Override
    public String getName() {
        return TYPE;
    }

    @Override
    public EndpointDefinition getStartDefinition() {
        return new EndpointDefinition(S3BucketSchema.TYPE, "itsS3AccessPolicy");
    }

    @Override
    public EndpointDefinition getEndDefinition() {
        return new EndpointDefinition(PolicySchema.TYPE, "itsS3Entity", 2);
    }

}
