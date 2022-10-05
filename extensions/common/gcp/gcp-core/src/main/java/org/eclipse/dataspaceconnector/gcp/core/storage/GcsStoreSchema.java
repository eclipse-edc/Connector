/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.gcp.core.storage;

public class GcsStoreSchema {
    public static final String TYPE = "GoogleCloudStorage";
    public static final String BUCKET_NAME = "bucket_name";
    public static final String LOCATION = "location";
    public static final String STORAGE_CLASS = "storage_class";
    public static final String SERVICE_ACCOUNT_NAME = "service_account_name";
    public static final String SERVICE_ACCOUNT_EMAIL = "service_account_email";
    public static final String BLOB_NAME = "blob_name";

    private GcsStoreSchema() {
    }
}
