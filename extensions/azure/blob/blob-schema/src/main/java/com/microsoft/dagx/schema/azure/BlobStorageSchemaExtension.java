/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.schema.azure;

import com.microsoft.dagx.schema.SchemaRegistry;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

public class BlobStorageSchemaExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var sr= context.getService(SchemaRegistry.class);
        sr.register(new AzureBlobStoreSchema());
        sr.register(new AzureBlobHasPolicyRelationshipSchema());

        monitor.info("Initialized Azure Blob Schemas");

    }
}

