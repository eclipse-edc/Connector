/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.test.system.local;

import com.azure.storage.blob.BlobServiceClient;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.transfer.spi.types.TransferType;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.test.system.utils.TransferConfiguration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONTRACT_VALIDITY;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_ASSET_FILE;

public class BlobTransferConfiguration implements TransferConfiguration {

    static final String BLOB_CONTENT = "Test blob content";
    private final String consumerManagementUrl;
    private final String providerIdsUrl;
    private final BlobServiceClient blobServiceClient;
    private final int maxSeconds;

    public BlobTransferConfiguration(String consumerManagementUrl, String providerIdsUrl, BlobServiceClient blobServiceClient, int maxSeconds) {
        this.consumerManagementUrl = consumerManagementUrl;
        this.providerIdsUrl = providerIdsUrl;
        this.blobServiceClient = blobServiceClient;
        this.maxSeconds = maxSeconds;
    }

    @Override
    public String getConsumerManagementUrl() {
        return consumerManagementUrl;
    }

    @Override
    public String getProviderIdsUrl() {
        return providerIdsUrl;
    }

    @Override
    public NegotiationInitiateRequestDto createNegotiationRequest(ContractOffer offer) {
        var policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .target(offer.getAsset().getId())
                        .action(Action.Builder.newInstance().type("USE").build())
                        .build())
                .type(PolicyType.SET)
                .build();

        var offerDescription = ContractOfferDescription.Builder.newInstance()
                .offerId(offer.getId())
                .assetId(offer.getAsset().getId())
                .policy(policy)
                .validity(CONTRACT_VALIDITY)
                .build();

        return NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorId("consumer")
                .consumerId("urn:connector:consumer")
                .providerId("urn:connector:provider")
                .connectorAddress(providerIdsUrl)
                .protocol("ids-multipart")
                .offer(offerDescription)
                .build();
    }

    @Override
    public TransferRequestDto createTransferRequest(ContractOffer offer, String contractAgreementId) {
        return TransferRequestDto.Builder.newInstance()
                .assetId(offer.getAsset().getId())
                .connectorId("consumer")
                .protocol("ids-multipart")
                .managedResources(true)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type(AzureBlobStoreSchema.TYPE)
                        .property(AzureBlobStoreSchema.ACCOUNT_NAME, blobServiceClient.getAccountName())
                        .build())
                .transferType(TransferType.Builder.transferType()
                        .contentType("application/octet-stream")
                        .isFinite(true)
                        .build())
                .connectorAddress(providerIdsUrl)
                .contractId(contractAgreementId)
                .build();
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
