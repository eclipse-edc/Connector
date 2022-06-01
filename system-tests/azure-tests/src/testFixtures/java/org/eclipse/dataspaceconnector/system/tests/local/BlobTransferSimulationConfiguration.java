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

package org.eclipse.dataspaceconnector.system.tests.local;

import com.azure.storage.blob.BlobServiceClient;
import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.eclipse.dataspaceconnector.system.tests.utils.TransferInitiationData;
import org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationConfiguration;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.azure.testfixtures.AbstractAzureBlobTest.getBlobServiceClient;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.PROVIDER_ASSET_FILE;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.PROVIDER_ASSET_ID;

/**
 * Configuration for Azure Blob transfer used in
 * {@link org.eclipse.dataspaceconnector.system.tests.local.BlobTransferLocalSimulation}.
 */
public class BlobTransferSimulationConfiguration implements TransferSimulationConfiguration {

    private final BlobServiceClient blobServiceClient;
    private final int maxSeconds;
    static final String BLOB_CONTENT = Faker.instance().lorem().sentence();

    public BlobTransferSimulationConfiguration(String accountName, String accountKey, String accountEndpoint, int maxSeconds) {
        this.blobServiceClient = getBlobServiceClient(accountName, accountKey, accountEndpoint);
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
    public boolean verifyTransferResult(Map<String, String> dataDestinationProperties) {
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
