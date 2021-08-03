package org.eclipse.edc.schema.s3;


import org.eclipse.edc.schema.RelationshipSchema;
import org.eclipse.edc.schema.policy.PolicySchema;

public class AmazonS3HasPolicyRelationshipSchema extends RelationshipSchema {
    public static final String TYPE = "edc:schema:relation:amazons3_has_policy";


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
