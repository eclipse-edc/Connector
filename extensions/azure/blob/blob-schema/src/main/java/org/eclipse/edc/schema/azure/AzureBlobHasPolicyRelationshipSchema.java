package org.eclipse.edc.schema.azure;

import org.eclipse.edc.schema.RelationshipSchema;
import org.eclipse.edc.schema.policy.PolicySchema;

public class AzureBlobHasPolicyRelationshipSchema extends RelationshipSchema {
    public static final String TYPE = "edc:schema:relation:azureblob_has_policy";


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
