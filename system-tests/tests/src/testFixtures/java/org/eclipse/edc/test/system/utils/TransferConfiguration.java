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

import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;

import java.util.Map;

/**
 * Configuration for a transfer E2E test. It provides pointers for the consumer API and the provider IDS url.
 * Implementors should also provide DTO for making requests in the transfer flow.
 */

public interface TransferConfiguration {

    String getConsumerManagementUrl();

    String getProviderIdsUrl();

    NegotiationInitiateRequestDto createNegotiationRequest(ContractOffer offer);

    TransferRequestDto createTransferRequest(ContractOffer offer, String contractAgreementId);

    default boolean isTransferResultValid(Map<String, String> dataDestinationProperties) {
        return true;
    }
}
