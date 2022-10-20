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

package org.eclipse.edc.test.system.local;

import com.azure.storage.blob.BlobServiceClient;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.testfixtures.TestFunctions;
import org.eclipse.edc.connector.transfer.spi.types.TransferType;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.test.system.utils.TransferInitiationData;
import org.eclipse.edc.test.system.utils.TransferSimulationConfiguration;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.test.system.utils.TransferSimulationUtils.PROVIDER_ASSET_FILE;
import static org.eclipse.edc.test.system.utils.TransferSimulationUtils.PROVIDER_ASSET_ID;

/**
 * Configuration for Azure Blob transfer used in
 * {@link BlobTransferLocalSimulation}.
 */
public class BlobTransferSimulationConfiguration implements TransferSimulationConfiguration {

    static final String BLOB_CONTENT = "Test blob content";
    private final BlobServiceClient blobServiceClient;
    private final int maxSeconds;

    public BlobTransferSimulationConfiguration(String accountName, String accountKey, String accountEndpoint, int maxSeconds) {
        this.blobServiceClient = TestFunctions.getBlobServiceClient(accountName, accountKey, accountEndpoint);
        this.maxSeconds = maxSeconds;
    }

    @Override
    public String createTransferRequest(TransferInitiationData transferInitiationData) {
        var request = Map.of(
                "contractId", transferInitiationData.contractAgreementId,
                "assetId", PROVIDER_ASSET_ID,
                "connectorId", "consumer",
                "connectorAddress", transferInitiationData.connectorAddress,
                "protocol", "ids-multipart",
                "dataDestination", DataAddress.Builder.newInstance()
                        .type(AzureBlobStoreSchema.TYPE)
                        .property(AzureBlobStoreSchema.ACCOUNT_NAME, blobServiceClient.getAccountName())
                        .build(),
                "managedResources", true,
                "transferType", TransferType.Builder.transferType()
                        .contentType("application/octet-stream")
                        .isFinite(true)
                        .build()
        );

        return new TypeManager().writeValueAsString(request);
    }

    public Duration copyMaxDuration() {
        return Duration.ofSeconds(maxSeconds);
    }

    @Override
    public boolean isTransferResultValid(Map<String, String> dataDestinationProperties) {
        // Assert
        var container = dataDestinationProperties.get("container");
        var destinationBlob = blobServiceClient.getBlobContainerClient(container)
                .getBlobClient(PROVIDER_ASSET_FILE);
        assertThat(destinationBlob.exists())
                .withFailMessage("Destination blob %s not created", destinationBlob.getBlobUrl())
                .isTrue();
        var actualBlobContent = destinationBlob.downloadContent().toString();
        assertThat(actualBlobContent)
                .withFailMessage("Transferred file contents are not same as the source file")
                .isEqualTo(BLOB_CONTENT);
        return true;
    }
}
