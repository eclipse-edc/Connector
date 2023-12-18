/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Daimler TSS GmbH - fixed contract dates to epoch seconds
 *
 */

package org.eclipse.edc.connector.defaults.storage.contractnegotiation;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TestFunctions {

    public static ContractNegotiation createNegotiation(String id) {
        return createNegotiationBuilder(id)
                .contractAgreement(createAgreement())
                .build();
    }

    public static ContractNegotiation.Builder createNegotiationBuilder(String id) {
        return ContractNegotiation.Builder.newInstance()
                .id(id)
                .type(ContractNegotiation.Type.CONSUMER)
                .contractOffers(List.of(ContractOffer.Builder.newInstance().id("contractId")
                        .policy(Policy.Builder.newInstance().build())
                        .build()))
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("protocol")
                .correlationId("correlationId");
    }

    public static ContractAgreement createAgreement() {
        return createAgreementBuilder()
                .build();
    }

    public static ContractAgreement.Builder createAgreementBuilder() {
        return ContractAgreement.Builder.newInstance()
                .id("agreementId")
                .providerId("provider")
                .consumerId("consumer")
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .contractSigningDate(Instant.now().getEpochSecond());
    }
}
