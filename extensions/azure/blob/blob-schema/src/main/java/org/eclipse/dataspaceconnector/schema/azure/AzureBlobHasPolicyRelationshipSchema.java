package org.eclipse.dataspaceconnector.schema.azure;

import org.eclipse.dataspaceconnector.schema.RelationshipSchema;
import org.eclipse.dataspaceconnector.schema.policy.PolicySchema;

public class AzureBlobHasPolicyRelationshipSchema extends RelationshipSchema {
    public static final String TYPE = "dataspaceconnector:schema:relation:azureblob_has_policy";


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
