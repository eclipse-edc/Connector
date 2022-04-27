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

package org.eclipse.dataspaceconnector.azure.blob.core;


/**
 * Constants used in Azure Blob storage data address properties.
 */
public class AzureBlobStoreSchema {

    private AzureBlobStoreSchema() {
    }

    public static final String TYPE = "AzureStorage";
    public static final String CONTAINER_NAME = "container";
    public static final String ACCOUNT_NAME = "account";
    public static final String BLOB_NAME = "blobname";
    public static final String SHARED_KEY = "sharedKey";
}
