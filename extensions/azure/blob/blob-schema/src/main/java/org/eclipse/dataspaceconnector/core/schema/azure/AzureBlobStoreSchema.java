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


import org.eclipse.dataspaceconnector.core.schema.DataSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.schema.SchemaAttribute;

public class AzureBlobStoreSchema extends DataSchema {

    public static final String TYPE = "AzureStorage";
    public static final String CONTAINER_NAME = "container";
    public static final String ACCOUNT_NAME = "account";
    public static final String BLOB_NAME = "blobname";

    @Override
    protected void addAttributes() {
        attributes.add(new SchemaAttribute(ACCOUNT_NAME, true));
        attributes.add(new SchemaAttribute(BLOB_NAME, false));
        attributes.add(new SchemaAttribute(CONTAINER_NAME, true));
    }

    @Override
    public String getName() {
        return TYPE;
    }
}
