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

package org.eclipse.dataspaceconnector.catalog.atlas.metadata;

import org.eclipse.dataspaceconnector.schema.SchemaAttribute;

public class AtlasCustomTypeAttribute extends SchemaAttribute {
    public static final String ATLAS_TYPE_STRING = "string";
    public static final String AZURE_BLOB_ACCOUNT = "account";
    public static final String AZURE_BLOB_BLOBNAME = "blobname";
    public static final String AZURE_BLOB_CONTAINER = "container";
    public static final String EDC_STORAGE_TYPE = "type";
    public static final String EDC_STORAGE_KEYNAME = "keyName";

    public AtlasCustomTypeAttribute(String name, String type, boolean required) {
        super(name, type, required);
    }

    public AtlasCustomTypeAttribute() {
        super();
    }


}
