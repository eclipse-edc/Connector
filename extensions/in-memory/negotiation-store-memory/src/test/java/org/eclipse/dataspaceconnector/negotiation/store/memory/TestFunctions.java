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

package org.eclipse.dataspaceconnector.negotiation.store.memory;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public class TestFunctions {

    public static ContractNegotiation createNegotiation(String name) {
        return ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id(name)
                .contractAgreement(createAgreement())
                .contractOffers(List.of(ContractOffer.Builder.newInstance().id("contractId")
                        .policy(Policy.Builder.newInstance().build()).build()))
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("ids-multipart")
                .build();
    }

    public static ContractAgreement createAgreement() {
        return ContractAgreement.Builder.newInstance()
                .id("agreementId")
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .contractStartDate(Instant.now().getEpochSecond())
                .contractEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                .contractSigningDate(Instant.now().getEpochSecond())
                .build();
    }
}
