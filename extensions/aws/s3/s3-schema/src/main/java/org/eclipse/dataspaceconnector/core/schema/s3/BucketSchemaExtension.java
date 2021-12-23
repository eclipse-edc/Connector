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

import org.eclipse.dataspaceconnector.spi.schema.SchemaRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class BucketSchemaExtension implements ServiceExtension {

    @Override
    public String name() {
        return "S3 Bucket Schemas";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var sr = context.getService(SchemaRegistry.class);
        sr.register(new S3BucketSchema());
        sr.register(new AmazonS3HasPolicyRelationshipSchema());
    }
}

