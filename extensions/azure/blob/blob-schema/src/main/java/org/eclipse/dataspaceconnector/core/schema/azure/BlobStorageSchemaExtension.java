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

import org.eclipse.dataspaceconnector.spi.schema.SchemaRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class BlobStorageSchemaExtension implements ServiceExtension {

    @Override
    public String name() {
        return "Azure Blob Schemas";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var sr = context.getService(SchemaRegistry.class);
        sr.register(new AzureBlobStoreSchema());
        sr.register(new AzureBlobHasPolicyRelationshipSchema());
    }
}

