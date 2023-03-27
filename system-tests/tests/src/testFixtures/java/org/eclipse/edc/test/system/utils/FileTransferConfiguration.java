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

package org.eclipse.edc.test.system.utils;

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

import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONTRACT_VALIDITY;

public class FileTransferConfiguration implements TransferConfiguration {


    private final String consumerManagementUrl;
    private final String providerIdsUrl;
    private final String destinationPath;

    public FileTransferConfiguration(String consumerManagementUrl, String providerIdsUrl, String destinationPath) {
        this.consumerManagementUrl = consumerManagementUrl;
        this.providerIdsUrl = providerIdsUrl;
        this.destinationPath = destinationPath;
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
                .managedResources(false)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("File")
                        .property("path", destinationPath)
                        .build())
                .transferType(TransferType.Builder.transferType()
                        .contentType("application/octet-stream")
                        .isFinite(true)
                        .build())
                .connectorAddress(providerIdsUrl)
                .contractId(contractAgreementId)
                .build();
    }
}
