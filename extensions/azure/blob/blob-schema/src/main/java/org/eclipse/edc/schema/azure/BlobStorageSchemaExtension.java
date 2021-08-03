/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.schema.azure;

import org.eclipse.edc.schema.SchemaRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

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

