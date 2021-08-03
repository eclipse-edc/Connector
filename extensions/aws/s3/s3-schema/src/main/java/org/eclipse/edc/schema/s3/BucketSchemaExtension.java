/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.schema.s3;

import org.eclipse.edc.schema.SchemaRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

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

