/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.core.schema.azure;

import org.eclipse.dataspaceconnector.core.schema.RelationshipSchema;
import org.eclipse.dataspaceconnector.core.schema.policy.PolicySchema;

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
