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

package org.eclipse.edc.connector.dataplane.azure.datafactory;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.ms.dataverse.MicrosoftDataverseSchema;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createDataAddress;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;

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

    public static DataFlowRequest createBlobToBlobRequest() {
        return createRequest(AzureBlobStoreSchema.TYPE)
                .sourceDataAddress(createDataAddress(AzureBlobStoreSchema.TYPE).properties(sourceProperties()).build())
                .destinationDataAddress(createDataAddress(AzureBlobStoreSchema.TYPE).properties(destinationProperties()).build())
                .build();
    }
    public static DataFlowRequest createBlobToDataverseRequest() {
        return DataFlowRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(createDataAddress(AzureBlobStoreSchema.TYPE).build())
                .destinationDataAddress(createDataAddress(MicrosoftDataverseSchema.TYPE).build()).build();
    }
}
