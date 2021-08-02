/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.schema.s3;

import com.microsoft.dagx.schema.SchemaRegistry;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

public class BucketSchemaExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var sr= context.getService(SchemaRegistry.class);
        sr.register(new S3BucketSchema());
        sr.register(new AmazonS3HasPolicyRelationshipSchema());

        monitor.info("Initialized S3 Bucket Schemas");

    }
}

