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

package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory;

import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.util.Map;

import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createDataAddress;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureStorageTestFixtures.createRequest;

public class TestFunctions {

    public static Map<String, String> sourceProperties() {
        var srcStorageAccount = createAccountName();
        return Map.of(
                AzureBlobStoreSchema.ACCOUNT_NAME, srcStorageAccount,
                AzureBlobStoreSchema.CONTAINER_NAME, createContainerName(),
                AzureBlobStoreSchema.BLOB_NAME, createBlobName(),
                DataAddress.KEY_NAME, srcStorageAccount + "-key1");
    }

    public static Map<String, String> destinationProperties() {
        var destStorageAccount = createAccountName();

        return Map.of(
                AzureBlobStoreSchema.ACCOUNT_NAME, destStorageAccount,
                AzureBlobStoreSchema.CONTAINER_NAME, createContainerName(),
                DataAddress.KEY_NAME, destStorageAccount + "-key1");
    }

    public static DataFlowRequest createFlowRequest() {
        return createRequest(AzureBlobStoreSchema.TYPE)
                .sourceDataAddress(createDataAddress(AzureBlobStoreSchema.TYPE).properties(sourceProperties()).build())
                .destinationDataAddress(createDataAddress(AzureBlobStoreSchema.TYPE).properties(destinationProperties()).build())
                .build();
    }
}
