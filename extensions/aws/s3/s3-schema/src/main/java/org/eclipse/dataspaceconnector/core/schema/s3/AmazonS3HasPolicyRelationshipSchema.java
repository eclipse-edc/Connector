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

package org.eclipse.dataspaceconnector.core.schema.s3;


import org.eclipse.dataspaceconnector.core.schema.RelationshipSchema;
import org.eclipse.dataspaceconnector.core.schema.policy.PolicySchema;

public class AmazonS3HasPolicyRelationshipSchema extends RelationshipSchema {
    public static final String TYPE = "dataspaceconnector:schema:relation:amazons3_has_policy";


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
