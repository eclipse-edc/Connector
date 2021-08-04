/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.schema.s3;

import org.eclipse.dataspaceconnector.schema.SchemaRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

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

