/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.schema.azure;

import org.eclipse.dataspaceconnector.schema.SchemaRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

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

