/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.testfixtures;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.jetbrains.annotations.NotNull;

public final class TestFunctions {
    private TestFunctions() {
    }

    @NotNull
    public static BlobServiceClient getBlobServiceClient(String accountName, String key) {
        return getBlobServiceClient(accountName, key, getBlobServiceTestEndpoint(accountName));
    }

    @NotNull
    public static BlobServiceClient getBlobServiceClient(String accountName, String key, String endpoint) {
        var client = new BlobServiceClientBuilder()
                .credential(new StorageSharedKeyCredential(accountName, key))
                .endpoint(endpoint)
                .buildClient();

        client.getAccountInfo();
        return client;
    }

    @NotNull
    public static String getBlobServiceTestEndpoint(String accountName) {
        return "http://127.0.0.1:10000/" + accountName;
    }
}
