package com.microsoft.dagx.schema.azure;

import com.microsoft.dagx.schema.RelationshipSchema;
import com.microsoft.dagx.schema.policy.PolicySchema;

public class AzureBlobHasPolicyRelationshipSchema extends RelationshipSchema {
    public static final String TYPE = "dagx:schema:relation:azureblob_has_policy";


    @Override
    public String getName() {
        return TYPE;
    }

    @Override
    public EndpointDefinition getStartDefinition() {
        return new EndpointDefinition(AzureBlobStoreSchema.TYPE, "itsAccessPolicy");
    }

    @Override
    public EndpointDefinition getEndDefinition() {
        return new EndpointDefinition(PolicySchema.TYPE, "itsEntity", 2);
    }

}
